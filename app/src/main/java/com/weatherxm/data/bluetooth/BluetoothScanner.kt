package com.weatherxm.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import arrow.core.Either
import com.espressif.provisioning.ESPProvisionManager
import com.espressif.provisioning.listeners.BleScanListener
import com.weatherxm.data.BluetoothError
import com.weatherxm.data.Failure
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import timber.log.Timber

class BluetoothScanner(private val espProvisionManager: ESPProvisionManager) {
    private val scannedDevices = MutableSharedFlow<BluetoothDevice>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val completionStatus = MutableSharedFlow<Either<Failure, Unit>>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_LATEST
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    fun registerOnScanning(): Flow<BluetoothDevice> {
        completionStatus.resetReplayCache()
        return scannedDevices
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getCompletionStatus(): Flow<Either<Failure, Unit>> {
        completionStatus.resetReplayCache()
        return completionStatus
    }

    /*
    * Suppress this because we have asked for permissions already before we reach here.
    */
    @SuppressLint("MissingPermission")
    fun startScanning() {
        espProvisionManager.searchBleEspDevices(object : BleScanListener {
            override fun scanStartFailed() {
                completionStatus.tryEmit(Either.Left(BluetoothError.ScanningError))
            }

            override fun onPeripheralFound(device: BluetoothDevice?, scanResult: ScanResult?) {
                device?.let {
                    scannedDevices.tryEmit(it)
                    // TODO: Add filtering with the correct one in the future
//                    if (it.name.contains("WXM")) {
//                        scannedDevices.tryEmit(it)
//                    }
                }
            }

            override fun scanCompleted() {
                completionStatus.tryEmit(Either.Right(Unit))
            }

            override fun onFailure(e: Exception?) {
                Timber.w(e, "BLE Scanning failure.")
                completionStatus.tryEmit(Either.Left(BluetoothError.ScanningError))
            }
        })
    }
}
