package com.weatherxm.data.bluetooth

import android.bluetooth.BluetoothDevice
import arrow.core.Either
import com.espressif.provisioning.ESPConstants
import com.espressif.provisioning.ESPDevice
import com.espressif.provisioning.WiFiAccessPoint
import com.espressif.provisioning.listeners.ProvisionListener
import com.espressif.provisioning.listeners.WiFiScanListener
import com.weatherxm.data.BluetoothError
import com.weatherxm.data.Failure
import timber.log.Timber
import kotlin.coroutines.suspendCoroutine

class BluetoothProvisioner(private val espDevice: ESPDevice) {
    fun connectBLEDevice(bluetoothDevice: BluetoothDevice) {
        // TODO: Replace primary service Uuid with the correct one
        espDevice.connectBLEDevice(bluetoothDevice, "REPLACE_THIS")
    }

    suspend fun scanNetworks(): Either<Failure, ArrayList<WiFiAccessPoint>?> {
        return suspendCoroutine { continuation ->
            espDevice.scanNetworks(object : WiFiScanListener {
                override fun onWifiListReceived(wifiList: ArrayList<WiFiAccessPoint>?) {
                    continuation.resumeWith(Result.success(Either.Right(wifiList)))
                }

                override fun onWiFiScanFailed(e: java.lang.Exception?) {
                    Timber.w(e, "[Provision]: Wifi Scan Failed.")
                    continuation.resumeWith(
                        Result.success(Either.Left(BluetoothError.ProvisionError.WifiScanError))
                    )
                }
            })
        }
    }

    suspend fun provisionDevice(ssid: String, passphrase: String): Either<Failure, Unit> {
        return suspendCoroutine { continuation ->
            espDevice.provision(ssid, passphrase, object : ProvisionListener {
                override fun createSessionFailed(e: Exception?) {
                    Timber.w(e, "[Provision]: creating session failed.")
                    continuation.resumeWith(
                        Result.success(
                            Either.Left(BluetoothError.ProvisionError.CreateSessionError)
                        )
                    )
                }

                override fun wifiConfigSent() {
                    Timber.d("[Provision]: wifi config sent")
                }

                override fun wifiConfigFailed(e: Exception?) {
                    Timber.w(e, "[Provision]: WiFi config failed")
                    continuation.resumeWith(
                        Result.success(Either.Left(BluetoothError.ProvisionError.WifiConfigError))
                    )
                }

                override fun wifiConfigApplied() {
                    Timber.d("[Provision]: wifi config applied")
                }

                override fun wifiConfigApplyFailed(e: Exception?) {
                    Timber.w(e, "[Provision]: Applying wifi config failed")
                    continuation.resumeWith(
                        Result.success(Either.Left(BluetoothError.ProvisionError.WifiConfigError))
                    )
                }

                override fun provisioningFailedFromDevice(
                    failureReason: ESPConstants.ProvisionFailureReason?
                ) {
                    Timber.w("[Provision]: Failed from device $failureReason")
                    continuation.resumeWith(
                        Result.success(Either.Left(BluetoothError.ProvisionError.GenericError))
                    )
                }

                override fun deviceProvisioningSuccess() {
                    Timber.d("[Provision]: Success")
                    continuation.resumeWith(Result.success(Either.Right(Unit)))
                }

                override fun onProvisioningFailed(e: Exception?) {
                    Timber.w(e, "[Provision]: Failed")
                }
            })
        }
    }
}
