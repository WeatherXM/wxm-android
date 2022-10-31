package com.weatherxm.data.bluetooth

import android.content.Context
import android.net.Uri
import android.os.Build
import com.juul.kable.identifier
import com.weatherxm.service.DfuService
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import no.nordicsemi.android.dfu.DfuProgressListener
import no.nordicsemi.android.dfu.DfuServiceInitiator
import no.nordicsemi.android.dfu.DfuServiceListenerHelper
import org.koin.core.component.KoinComponent
import timber.log.Timber

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
    fun setUpdater() {
        dfuServiceInitiator =
            DfuServiceInitiator(bluetoothConnectionManager.getPeripheral().identifier)
                .setKeepBond(true)
                .setPrepareDataObjectDelay(300L)

        DfuServiceListenerHelper.registerProgressListener(
            context, object : DfuProgressListener {
                override fun onDeviceConnecting(deviceAddress: String) {
                    Timber.d("[BLE Updater]: onDeviceConnecting: $deviceAddress")
                }

                override fun onDeviceConnected(deviceAddress: String) {
                    Timber.d("[BLE Updater]: onDeviceConnected: $deviceAddress")
                }

                override fun onDfuProcessStarting(deviceAddress: String) {
                    Timber.d("[BLE Updater]: onDfuProcessStarting: $deviceAddress")
                }

                override fun onDfuProcessStarted(deviceAddress: String) {
                    Timber.d("[BLE Updater]: onDfuProcessStarted: $deviceAddress")
                }

                override fun onEnablingDfuMode(deviceAddress: String) {
                    Timber.d("[BLE Updater]: onEnablingDfuMode: $deviceAddress")
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
                        "[BLE Updater]: onProgressChanged: $deviceAddress, " +
                            "percent: $percent%, speed: $speed, avgSpeed: $avgSpeed, " +
                                "currentPart: $currentPart, partsTotal: $partsTotal"
                        )
                        progress.tryEmit(percent)
                    }

                    override fun onFirmwareValidating(deviceAddress: String) {
                        Timber.d("[BLE Updater]: onFirmwareValidating: $deviceAddress")
                    }

                    override fun onDeviceDisconnecting(deviceAddress: String?) {
                        Timber.d("[BLE Updater]: onDeviceDisconnecting: $deviceAddress")
                    }

                    override fun onDeviceDisconnected(deviceAddress: String) {
                        Timber.d("[BLE Updater]: onDeviceDisconnected: $deviceAddress")
                    }

                    override fun onDfuCompleted(deviceAddress: String) {
                        Timber.d("[BLE Updater]: onDfuCompleted: $deviceAddress")
                    }

                    override fun onDfuAborted(deviceAddress: String) {
                        Timber.d("[BLE Updater]: onDfuAborted: $deviceAddress")
                    }

                    override fun onError(
                        deviceAddress: String,
                        error: Int,
                        errorType: Int,
                        message: String?
                    ) {
                        Timber.w(
                            "[BLE Updater]: onError: $deviceAddress, " +
                                "error: $error, errorType: $errorType, message: $message"
                        )
                    }
                })
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
