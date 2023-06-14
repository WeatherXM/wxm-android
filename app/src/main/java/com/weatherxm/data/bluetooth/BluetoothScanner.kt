package com.weatherxm.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import android.os.CountDownTimer
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
    companion object {
        const val DURATION = 5000L
        const val COUNTDOWN_INTERVAL = 1000L
    }

    private val scannedDevices = MutableSharedFlow<BluetoothDevice>(
        replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val progress = MutableSharedFlow<Either<Failure, Int>>(
        replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST
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
    @OptIn(ExperimentalCoroutinesApi::class)
    @Suppress("MagicNumber")
    @SuppressLint("MissingPermission")
    fun startScanning(): Flow<Either<Failure, Int>> {
        progress.resetReplayCache()
        isScanningRunning = true
        object : CountDownTimer(DURATION, DURATION / COUNTDOWN_INTERVAL) {
            override fun onTick(msUntilDone: Long) {
                progress.tryEmit(
                    Either.Right((((DURATION - msUntilDone) * 100L) / DURATION).toInt())
                )
            }

            override fun onFinish() {
                stopScanning()
            }
        }.start()

        espProvisionManager.searchBleEspDevices(object : BleScanListener {
            override fun scanStartFailed() {
                isScanningRunning = false
                progress.tryEmit(Either.Left(BluetoothError.ScanningError()))
            }

            override fun onPeripheralFound(device: BluetoothDevice?, scanResult: ScanResult?) {
                device?.name?.let {
                    /**
                     * DfuTarg is shown up when device gets in a "bricked" state, such as when
                     * a user interrupts the updating. So we need to show it in case the user
                     * wants to retry updating it.
                     */
                    if (it.isNotEmpty() && it.contains("WeatherXM") || it.contains("DfuTarg")) {
                        scannedDevices.tryEmit(device)
                    }
                }
            }

            override fun scanCompleted() {
                isScanningRunning = false
                progress.tryEmit(Either.Right(100))
            }

            override fun onFailure(e: Exception?) {
                Timber.w(e, "[BLE Scanning]: Failure")
                isScanningRunning = false
                progress.tryEmit(Either.Left(BluetoothError.ScanningError()))
            }
        })
        return progress
    }

    /*
    * Suppress this because we have asked for permissions already before we reach here.
    */
    @SuppressLint("MissingPermission")
    fun stopScanning() {
        if (isScanningRunning) espProvisionManager.stopBleScan()
    }
}
