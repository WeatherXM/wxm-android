package com.weatherxm.data.bluetooth

import android.content.Context
import android.net.Uri
import android.os.Build
import com.weatherxm.R
import com.weatherxm.data.BluetoothOTAState
import com.weatherxm.data.OTAState
import com.weatherxm.service.DfuService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import no.nordicsemi.android.dfu.DfuProgressListener
import no.nordicsemi.android.dfu.DfuServiceInitiator
import no.nordicsemi.android.dfu.DfuServiceListenerHelper
import timber.log.Timber

class BluetoothUpdater(
    private val context: Context,
    private val bluetoothConnectionManager: BluetoothConnectionManager
) {
    private lateinit var dfuServiceInitiator: DfuServiceInitiator

    private val onOTAState = MutableSharedFlow<OTAState>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    @Suppress("MagicNumber")
    private fun setUpdater() {
        dfuServiceInitiator =
            DfuServiceInitiator(bluetoothConnectionManager.getPeripheral().identifier)
                .setKeepBond(true)
                .setPrepareDataObjectDelay(300L)

        DfuServiceListenerHelper.registerProgressListener(context, object : DfuProgressListener {
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
                onOTAState.tryEmit(OTAState(BluetoothOTAState.IN_PROGRESS, percent))
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
                onOTAState.tryEmit(OTAState(BluetoothOTAState.COMPLETED, 100))
            }

            override fun onDfuAborted(deviceAddress: String) {
                Timber.d("[BLE Updater]: onDfuAborted: $deviceAddress")
                onOTAState.tryEmit(OTAState(BluetoothOTAState.ABORTED, 0))
            }

            override fun onError(
                deviceAddress: String,
                error: Int,
                errorType: Int,
                message: String?
            ) {
                Timber.e(
                    "[BLE Updater]: onError: $deviceAddress, " +
                        "error: $error, errorType: $errorType, message: $message"
                )
                onOTAState.tryEmit(
                    OTAState(
                        BluetoothOTAState.FAILED,
                        0,
                        error,
                        errorType,
                        createErrorMessage(error, message)
                    )
                )
            }
        })
    }

    @Suppress("MagicNumber")
    private fun createErrorMessage(errorCode: Int, defaultMessage: String?): String? {
        return when (errorCode) {
            133 -> {
                "$defaultMessage - ${context.getString(R.string.error_helium_ota_133_suffix)}"
            }
            else -> defaultMessage
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun update(updatePackage: Uri): Flow<OTAState> {
        onOTAState.resetReplayCache()
        setUpdater()

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
        return onOTAState
    }
}
