package com.weatherxm.ui.components

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.data.BluetoothError
import com.weatherxm.data.Failure
import com.weatherxm.ui.common.ScannedDevice
import com.weatherxm.usecases.BluetoothConnectionUseCase
import com.weatherxm.usecases.BluetoothScannerUseCase
import com.weatherxm.util.Analytics
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

open class BluetoothHeliumViewModel(
    private val deviceBleAddress: String,
    private val scanUseCase: BluetoothScannerUseCase?,
    protected val connectionUseCase: BluetoothConnectionUseCase,
    protected val analytics: Analytics
) : ViewModel() {
    protected var scannedDevice = ScannedDevice.empty()

    init {
        viewModelScope.launch {
            connectionUseCase.registerOnBondStatus().collect {
                when (it) {
                    BluetoothDevice.BOND_BONDED -> {
                        onPaired()
                    }
                    BluetoothDevice.BOND_NONE -> {
                        analytics.trackEventFailure(Failure.CODE_BL_DEVICE_NOT_PAIRED)
                        onNotPaired()
                    }
                }
            }
        }
    }

    private val scanningJob: Job = viewModelScope.launch {
        scanUseCase?.registerOnScanning()?.collect {
            if (it.name?.contains(deviceBleAddress) == true) {
                scannedDevice = it
                scanUseCase.stopScanning()
            }
        }
    }

    open fun onPaired() {
        // To be overridden
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

    @Suppress("MagicNumber")
    protected fun scanAndConnect() {
        viewModelScope.launch {
            scanUseCase?.startScanning()?.collect {
                it.onRight { progress ->
                    if (progress == 100) {
                        setPeripheralAndConnect()
                    }
                }.onLeft { failure ->
                    analytics.trackEventFailure(failure.code)
                    onScanFailure(failure)
                }
            }
        }
    }

    protected suspend fun setPeripheralAndConnect(ignorePairing: Boolean = false) {
        if (scannedDevice == ScannedDevice.empty()) {
            onScanFailure(BluetoothError.DeviceNotFound)
            return
        }
        connectionUseCase.setPeripheral(scannedDevice.address).onRight {
            connect(ignorePairing)
        }.onLeft {
            analytics.trackEventFailure(it.code)
            onConnectionFailure(it)
        }
    }

    protected suspend fun connect(ignorePairing: Boolean = false) {
        if (scannedDevice == ScannedDevice.empty()) {
            onScanFailure(BluetoothError.DeviceNotFound)
            return
        }

        if (deviceIsPaired() && ignorePairing) {
            connectionUseCase.connectToPeripheral().onRight {
                onConnected()
            }.onLeft {
                analytics.trackEventFailure(it.code)
                onConnectionFailure(it)
            }
        } else {
            analytics.trackEventFailure(Failure.CODE_BL_DEVICE_NOT_PAIRED)
            onNotPaired()
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun disconnectFromPeripheral() {
        GlobalScope.launch {
            connectionUseCase.disconnectFromPeripheral()
        }
    }

    fun stopScanning() {
        scanUseCase?.stopScanning()
        scanningJob.cancel()
    }

    private fun deviceIsPaired(): Boolean {
        return connectionUseCase.getPairedDevices().any { it.address == scannedDevice.address }
    }
}
