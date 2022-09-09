package com.weatherxm.data.bluetooth

import android.bluetooth.BluetoothDevice
import arrow.core.Either
import com.espressif.provisioning.ESPConstants
import com.espressif.provisioning.ESPDevice
import com.espressif.provisioning.listeners.ProvisionListener
import com.espressif.provisioning.listeners.WiFiScanListener
import com.weatherxm.data.BluetoothError
import com.weatherxm.data.Failure
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.koin.core.component.KoinComponent
import timber.log.Timber

class BluetoothProvisioner(private val espDevice: ESPDevice) : KoinComponent {
    private val provisionCompletionStatus = MutableSharedFlow<Either<Failure, Unit>>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    // TODO: Use this to pass to the UI info about provisioning status (success or failure)
    fun getProvisioningCompletionStatus(): Flow<Either<Failure, Unit>> = provisionCompletionStatus

    fun connectBLEDevice(bluetoothDevice: BluetoothDevice) {
        // TODO: Replace primary service Uuid with the correct one
        espDevice.connectBLEDevice(bluetoothDevice, "REPLACE_THIS")
    }

    fun scanNetworks(wiFiScanListener: WiFiScanListener) {
        return espDevice.scanNetworks(wiFiScanListener)
    }

    fun provisionDevice(ssid: String, passphrase: String) {
        espDevice.provision(ssid, passphrase, object : ProvisionListener {
            override fun createSessionFailed(e: Exception?) {
                Timber.w(e, "Provision: creating session failed.")
                provisionCompletionStatus.tryEmit(
                    Either.Left(BluetoothError.ProvisionError.CreateSessionError())
                )
            }

            override fun wifiConfigSent() {
                Timber.d("Provision: wifi config sent")
            }

            override fun wifiConfigFailed(e: Exception?) {
                Timber.w(e, "Provision: WiFi config failed")
                provisionCompletionStatus.tryEmit(
                    Either.Left(BluetoothError.ProvisionError.WifiConfigError())
                )
            }

            override fun wifiConfigApplied() {
                Timber.d("Provision: wifi config applied")
            }

            override fun wifiConfigApplyFailed(e: Exception?) {
                Timber.w(e, "Provision: Applying wifi config failed")
                provisionCompletionStatus.tryEmit(
                    Either.Left(BluetoothError.ProvisionError.WifiConfigError())
                )
            }

            override fun provisioningFailedFromDevice(failureReason: ESPConstants.ProvisionFailureReason?) {
                Timber.w("Provision: Failed from device $failureReason")
                provisionCompletionStatus.tryEmit(
                    Either.Left(BluetoothError.ProvisionError.GenericError())
                )
            }

            override fun deviceProvisioningSuccess() {
                Timber.d("Provision: Success")
                provisionCompletionStatus.tryEmit(Either.Right(Unit))
            }

            override fun onProvisioningFailed(e: Exception?) {
                Timber.w(e, "Provision: Failed")
            }
        })
    }
}
