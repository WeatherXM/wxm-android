package com.weatherxm.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import arrow.core.Either
import com.espressif.provisioning.ESPProvisionManager
import com.espressif.provisioning.listeners.BleScanListener
import com.weatherxm.data.BluetoothDeviceWithEUI
import com.weatherxm.data.BluetoothError
import com.weatherxm.data.Failure
import com.weatherxm.data.toHexString
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import timber.log.Timber
import kotlin.coroutines.suspendCoroutine


class BluetoothScanner(private val espProvisionManager: ESPProvisionManager) {
    companion object {
        private const val DEVICE_EUI_MANUFACTURER_ID = 89
    }

    private val scannedDevices = MutableSharedFlow<BluetoothDeviceWithEUI>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    fun registerOnScanning(): Flow<BluetoothDeviceWithEUI> {
        scannedDevices.resetReplayCache()
        return scannedDevices
    }

    /*
    * Suppress this because we have asked for permissions already before we reach here.
    */
    @SuppressLint("MissingPermission")
    suspend fun startScanning(): Either<Failure, Unit> {
        return suspendCoroutine { continuation ->
            espProvisionManager.searchBleEspDevices(object : BleScanListener {
                override fun scanStartFailed() {
                    continuation.resumeWith(
                        Result.success(Either.Left(BluetoothError.ScanningError))
                    )
                }

                override fun onPeripheralFound(device: BluetoothDevice?, scanResult: ScanResult?) {
                    device?.let {
                        // TODO: Add filtering with the correct one in the future
                        if (it.name.contains("WXM")) {
                            val devEUI =
                                scanResult?.scanRecord?.getManufacturerSpecificData(
                                    DEVICE_EUI_MANUFACTURER_ID
                                )?.toHexString()?.uppercase()
                            scannedDevices.tryEmit(BluetoothDeviceWithEUI(devEUI, it))
                        }
                    }
                }

                override fun scanCompleted() {
                    continuation.resumeWith(
                        Result.success(Either.Right(Unit))
                    )
                }

                override fun onFailure(e: Exception?) {
                    Timber.w(e, "BLE Scanning failure.")
                    continuation.resumeWith(
                        Result.success(Either.Left(BluetoothError.ScanningError))
                    )
                }
            })
        }
    }
}
