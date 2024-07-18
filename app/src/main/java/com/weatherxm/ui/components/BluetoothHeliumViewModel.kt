package com.weatherxm.ui.components

import android.bluetooth.BluetoothDevice
import android.os.CountDownTimer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.BluetoothError
import com.weatherxm.data.Failure
import com.weatherxm.ui.common.ScannedDevice
import com.weatherxm.usecases.BluetoothConnectionUseCase
import com.weatherxm.usecases.BluetoothScannerUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

open class BluetoothHeliumViewModel(
    private val deviceBleAddress: String,
    private val scanUseCase: BluetoothScannerUseCase?,
    protected val connectionUseCase: BluetoothConnectionUseCase,
    protected val analytics: AnalyticsWrapper
) : ViewModel() {
    companion object {
        const val SCAN_DURATION = 5000L
        const val SCAN_COUNTDOWN_INTERVAL = 5L
    }

    protected var scannedDevice = ScannedDevice.empty()
    protected var scanningJob: Job? = null

    @Suppress("MagicNumber")
    protected open var timer = object : CountDownTimer(SCAN_DURATION, SCAN_COUNTDOWN_INTERVAL) {
        override fun onTick(msUntilDone: Long) {
            val progress = ((SCAN_DURATION - msUntilDone) * 100L / SCAN_DURATION).toInt()
            if (progress == 100) {
                viewModelScope.launch {
                    setPeripheralAndConnect()
                }
            }
        }

        override fun onFinish() {
            stopScanning()
        }
    }

    init {
        viewModelScope.launch {
            connectionUseCase.registerOnBondStatus().collect {
                when (it) {
                    BluetoothDevice.BOND_BONDED -> {
                        onConnected()
                    }
                    BluetoothDevice.BOND_NONE -> {
                        analytics.trackEventFailure(Failure.CODE_BL_DEVICE_NOT_PAIRED)
                        onNotPaired()
                    }
                }
            }
        }
    }

    open fun onNotPaired() {
        // To be overridden
    }

    open fun onScanFailure(failure: Failure) {
        // To be overridden
    }

    open fun onConnected() {
        // To be overridden
    }

    open fun onConnectionFailure(failure: Failure) {
        // To be overridden
    }

    protected fun scanAndConnect() {
        scanningJob = viewModelScope.launch {
            scanUseCase?.scan()?.collect {
                timer.start()
                if (it.name?.contains(deviceBleAddress) == true) {
                    scannedDevice = it
                    timer.cancel()
                    scanningJob?.cancel("Device Found: $deviceBleAddress")
                }
            }
        }
    }

    protected suspend fun setPeripheralAndConnect(ignorePairing: Boolean = false) {
        if (deviceNotScanned()) {
            return
        }
        connectionUseCase.setPeripheral(scannedDevice.address).onRight {
            connect(ignorePairing)
        }.onLeft {
            analytics.trackEventFailure(it.code)
            onConnectionFailure(it)
        }
    }

    suspend fun connect(ignorePairing: Boolean = false) {
        if (deviceNotScanned()) {
            return
        }
        if (deviceIsPaired()) {
            connectionUseCase.connectToPeripheral().onRight {
                onConnected()
            }.onLeft {
                analytics.trackEventFailure(it.code)
                onConnectionFailure(it)
            }
        } else if (ignorePairing) {
            /**
             * If we get here that means that device is not paired, so there will be a prompt
             * for pairing when trying to connect to the device, if the user accepts it then
             * BluetoothDevice.BOND_BONDED will get fired so we continue there
             */
            connectionUseCase.connectToPeripheral().onLeft {
                analytics.trackEventFailure(it.code)
                onConnectionFailure(it)
            }
        } else {
            analytics.trackEventFailure(Failure.CODE_BL_DEVICE_NOT_PAIRED)
            onNotPaired()
        }
    }

    fun disconnectFromPeripheral() {
        viewModelScope.launch {
            connectionUseCase.disconnectFromPeripheral()
        }
    }

    fun stopScanning() {
        if (scanningJob?.isActive == true) {
            scanningJob?.cancel()
            timer.cancel()
        }
    }

    private fun deviceIsPaired(): Boolean {
        return connectionUseCase.getPairedDevices().any { it.address == scannedDevice.address }
    }

    private fun deviceNotScanned(): Boolean {
        return if (scannedDevice == ScannedDevice.empty()) {
            onScanFailure(BluetoothError.DeviceNotFound)
            true
        } else {
            false
        }
    }
}
