package com.weatherxm.data.bluetooth

import android.content.Context
import android.net.Uri
import android.os.Build
import arrow.core.Either
import com.juul.kable.identifier
import com.weatherxm.data.BluetoothError
import com.weatherxm.data.Failure
import com.weatherxm.service.DfuService
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import no.nordicsemi.android.dfu.DfuProgressListener
import no.nordicsemi.android.dfu.DfuServiceInitiator
import no.nordicsemi.android.dfu.DfuServiceListenerHelper
import org.koin.core.component.KoinComponent
import timber.log.Timber
import kotlin.coroutines.suspendCoroutine

class BluetoothUpdater(
    private val context: Context,
    private val bluetoothConnectionManager: BluetoothConnectionManager
) : KoinComponent {
    private lateinit var dfuServiceInitiator: DfuServiceInitiator

    private val progress = MutableSharedFlow<Int>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    @Suppress("MagicNumber")
    suspend fun setUpdater(): Either<Failure, Unit> {
        return suspendCoroutine { continuation ->
            dfuServiceInitiator =
                DfuServiceInitiator(bluetoothConnectionManager.getPeripheral().identifier)
                    .setKeepBond(true)
                    .setPrepareDataObjectDelay(300L)

            DfuServiceListenerHelper.registerProgressListener(
                context,
                object : DfuProgressListener {
                    override fun onDeviceConnecting(deviceAddress: String) {
                        Timber.d("Updating via BLE: onDeviceConnecting: $deviceAddress")
                    }

                    override fun onDeviceConnected(deviceAddress: String) {
                        Timber.d("Updating via BLE: onDeviceConnected: $deviceAddress")
                    }

                    override fun onDfuProcessStarting(deviceAddress: String) {
                        Timber.d("Updating via BLE: onDfuProcessStarting: $deviceAddress")
                    }

                    override fun onDfuProcessStarted(deviceAddress: String) {
                        Timber.d("Updating via BLE: onDfuProcessStarted: $deviceAddress")
                    }

                    override fun onEnablingDfuMode(deviceAddress: String) {
                        Timber.d("Updating via BLE: onEnablingDfuMode: $deviceAddress")
                    }

                    override fun onProgressChanged(
                        deviceAddress: String,
                        percent: Int,
                        speed: Float,
                        avgSpeed: Float,
                        currentPart: Int,
                        partsTotal: Int
                    ) {
                        Timber.d(
                            "Updating via BLE: onProgressChanged: $deviceAddress, " +
                                "percent: $percent%, speed: $speed, avgSpeed: $avgSpeed, " +
                                "currentPart: $currentPart, partsTotal: $partsTotal"
                        )
                        progress.tryEmit(percent)
                    }

                    override fun onFirmwareValidating(deviceAddress: String) {
                        Timber.d("Updating via BLE: onFirmwareValidating: $deviceAddress")
                    }

                    override fun onDeviceDisconnecting(deviceAddress: String?) {
                        Timber.d("Updating via BLE: onDeviceDisconnecting: $deviceAddress")
                    }

                    override fun onDeviceDisconnected(deviceAddress: String) {
                        Timber.d("Updating via BLE: onDeviceDisconnected: $deviceAddress")
                    }

                    override fun onDfuCompleted(deviceAddress: String) {
                        Timber.d("Updating via BLE: onDfuCompleted: $deviceAddress")
                        continuation.resumeWith(
                            Result.success(Either.Right(Unit))
                        )
                    }

                    override fun onDfuAborted(deviceAddress: String) {
                        Timber.d("Updating via BLE: onDfuAborted: $deviceAddress")
                        continuation.resumeWith(
                            Result.success(Either.Left(BluetoothError.DfuAborted))
                        )
                    }

                    override fun onError(
                        deviceAddress: String,
                        error: Int,
                        errorType: Int,
                        message: String?
                    ) {
                        Timber.w(
                            "Updating via BLE: onError: $deviceAddress, " +
                                "error: $error, errorType: $errorType, message: $message"
                        )
                        continuation.resumeWith(
                            Result.success(Either.Left(BluetoothError.DfuUpdateError(message)))
                        )
                    }
                })
        }
    }

    fun update(updatePackage: Uri): Flow<Int> {
        /*
        * This is needed because of crashing. More:
        * https://github.com/NordicSemiconductor/Android-DFU-Library/issues/266
        * https://github.com/NordicSemiconductor/Android-DFU-Library/issues/106
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            DfuServiceInitiator.createDfuNotificationChannel(context)
        } else {
            dfuServiceInitiator.setForeground(false)
            dfuServiceInitiator.setDisableNotification(true)
        }

        dfuServiceInitiator.setZip(updatePackage)
        dfuServiceInitiator.start(context, DfuService::class.java)
        return progress
    }
}
