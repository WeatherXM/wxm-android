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
import kotlin.coroutines.suspendCoroutine


class BluetoothScanner(private val espProvisionManager: ESPProvisionManager) {
    private val scannedDevices = MutableSharedFlow<BluetoothDevice>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private var isScanningRunning = false

    @OptIn(ExperimentalCoroutinesApi::class)
    fun registerOnScanning(): Flow<BluetoothDevice> {
        scannedDevices.resetReplayCache()
        return scannedDevices
    }

    /*
    * Suppress this because we have asked for permissions already before we reach here.
    */
    @SuppressLint("MissingPermission")
    suspend fun startScanning(): Either<Failure, Unit> {
        return suspendCoroutine { continuation ->
            isScanningRunning = true
            espProvisionManager.searchBleEspDevices(object : BleScanListener {
                override fun scanStartFailed() {
                    isScanningRunning = false
                    continuation.resumeWith(
                        Result.success(Either.Left(BluetoothError.ScanningError))
                    )
                }

                override fun onPeripheralFound(device: BluetoothDevice?, scanResult: ScanResult?) {
                    device?.let {
                        /**
                         * DfuTarg is shown up when device gets in a "bricked" state, such as when
                         * a user interrupts the updating. So we need to show it in case the user
                         * wants to retry updating it.
                         */
                        val deviceName = it.name
                        if (deviceName.contains("WeatherXM") || deviceName.contains("DfuTarg")) {
                            scannedDevices.tryEmit(it)
                        }
                    }
                }

                override fun scanCompleted() {
                    isScanningRunning = false
                    continuation.resumeWith(
                        Result.success(Either.Right(Unit))
                    )
                }

                override fun onFailure(e: Exception?) {
                    Timber.w(e, "[BLE Scanning]: Failure")
                    isScanningRunning = false
                    continuation.resumeWith(
                        Result.success(Either.Left(BluetoothError.ScanningError))
                    )
                }
            })
        }
    }

    /*
    * Suppress this because we have asked for permissions already before we reach here.
    */
    @SuppressLint("MissingPermission")
    fun stopScanning() {
        if (isScanningRunning) espProvisionManager.stopBleScan()
    }
}
