package com.weatherxm.ui.claimdevice.helium.pair

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.data.BluetoothError
import com.weatherxm.data.Failure
import com.weatherxm.data.Resource
import com.weatherxm.ui.common.ScannedDevice
import com.weatherxm.ui.common.UIError
import com.weatherxm.ui.components.BluetoothHeliumViewModel
import com.weatherxm.usecases.BluetoothConnectionUseCase
import com.weatherxm.usecases.BluetoothScannerUseCase
import com.weatherxm.util.Analytics
import com.weatherxm.util.Resources
import kotlinx.coroutines.launch
import timber.log.Timber

class ClaimHeliumPairViewModel(
    private val resources: Resources,
    analytics: Analytics,
    private val scanDevicesUseCase: BluetoothScannerUseCase,
    connectionUseCase: BluetoothConnectionUseCase
) : BluetoothHeliumViewModel("", null, connectionUseCase, analytics) {
    private var scannedDevices: MutableList<ScannedDevice> = mutableListOf()

    private val onBLEError = MutableLiveData<UIError>()
    private val onBLEConnectionLost = MutableLiveData<Boolean>()
    private val onBLEConnection = MutableLiveData<Boolean>()
    private val onNewScannedDevice = MutableLiveData<List<ScannedDevice>>()
    private val onScanStatus = MutableLiveData<Resource<Unit>>()
    private val onScanProgress = MutableLiveData<Int>()

    private var bleConnectionStarted = false

    fun onNewScannedDevice(): LiveData<List<ScannedDevice>> = onNewScannedDevice
    fun onScanStatus(): LiveData<Resource<Unit>> = onScanStatus
    fun onScanProgress(): LiveData<Int> = onScanProgress
    fun onBLEError() = onBLEError
    fun onBLEConnectionLost() = onBLEConnectionLost
    fun onBLEConnection() = onBLEConnection

    @Suppress("MagicNumber")
    fun scanBleDevices() {
        viewModelScope.launch {
            onScanStatus.postValue(Resource.loading())
            scannedDevices.clear()
            scanDevicesUseCase.startScanning().collect {
                it.onRight { progress ->
                    if (progress == 100) {
                        onScanProgress.postValue(progress)
                        onScanStatus.postValue(Resource.success(Unit))
                    } else {
                        onScanProgress.postValue(progress)
                    }
                }.onLeft { failure ->
                    analytics.trackEventFailure(failure.code)
                    onScanStatus.postValue(Resource.error(""))
                }
            }
        }
    }

    override fun onPaired() {
        onBLEConnection.postValue(true)
    }

    override fun onNotPaired() {
        analytics.trackEventFailure(Failure.CODE_BL_DEVICE_NOT_PAIRED)
        onBLEError.postValue(
            UIError(resources.getString(R.string.helium_pairing_failed_desc)) {
                setupBluetoothClaiming(super.scannedDevice)
            }
        )
    }

    override fun onConnected() {
        if (connectionUseCase.getPairedDevices().any {
                it.address == super.scannedDevice.address
            }
        ) {
            onBLEConnection.postValue(true)
        }
    }

    override fun onConnectionFailure(failure: Failure) {
        bleConnectionStarted = false
        when (failure) {
            is BluetoothError.BluetoothDisabledException -> {
                onBLEError.postValue(
                    UIError(resources.getString(R.string.helium_bluetooth_disabled)) {
                        setupBluetoothClaiming(super.scannedDevice)
                    }
                )
            }
            is BluetoothError.ConnectionLostException -> {
                onBLEConnectionLost.postValue(true)
            }
            else -> {
                onBLEError.postValue(
                    UIError(resources.getString(R.string.helium_pairing_failed_desc)) {
                        setupBluetoothClaiming(super.scannedDevice)
                    }
                )
            }
        }
    }

    fun setupBluetoothClaiming(scannedDevice: ScannedDevice = super.scannedDevice) {
        if (bleConnectionStarted) return
        bleConnectionStarted = true

        super.stopScanning()
        super.scannedDevice = scannedDevice

        viewModelScope.launch {
            super.setPeripheralAndConnect(true)
        }
    }

    init {
        viewModelScope.launch {
            scanDevicesUseCase.registerOnScanning().collect {
                if (!scannedDevices.contains(it)) {
                    Timber.d("New scanned device collected: $it")
                    scannedDevices.add(it)
                    this@ClaimHeliumPairViewModel.onNewScannedDevice.postValue(scannedDevices)
                }
            }
        }
    }
}
