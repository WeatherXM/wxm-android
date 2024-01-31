package com.weatherxm.ui.claimdevice.helium.pair

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.data.BluetoothError
import com.weatherxm.data.Failure
import com.weatherxm.data.Resource
import com.weatherxm.ui.common.ScannedDevice
import com.weatherxm.ui.common.UIError
import com.weatherxm.ui.common.empty
import com.weatherxm.usecases.BluetoothConnectionUseCase
import com.weatherxm.usecases.BluetoothScannerUseCase
import com.weatherxm.util.Analytics
import com.weatherxm.util.Resources
import kotlinx.coroutines.launch
import timber.log.Timber

class ClaimHeliumPairViewModel(
    private val resources: Resources,
    private val analytics: Analytics,
    private val scanDevicesUseCase: BluetoothScannerUseCase,
    private val connectionUseCase: BluetoothConnectionUseCase
) : ViewModel() {
    private var scannedDevices: MutableList<ScannedDevice> = mutableListOf()

    private val onBLEError = MutableLiveData<UIError>()
    private val onBLEConnectionLost = MutableLiveData<Boolean>()
    private val onBLEConnection = MutableLiveData<Boolean>()
    private val onNewScannedDevice = MutableLiveData<List<ScannedDevice>>()
    private val onScanStatus = MutableLiveData<Resource<Unit>>()
    private val onScanProgress = MutableLiveData<Int>()

    private var selectedDeviceMacAddress: String = String.empty()
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
                    onScanStatus.postValue(Resource.error(String.empty()))
                }
            }
        }
    }

    fun setupBluetoothClaiming(macAddress: String) {
        if (bleConnectionStarted) return
        bleConnectionStarted = true

        scanDevicesUseCase.stopScanning()
        selectedDeviceMacAddress = macAddress
        connectionUseCase.setPeripheral(macAddress).onRight {
            connectToPeripheral()
        }.onLeft {
            analytics.trackEventFailure(it.code)
            bleConnectionStarted = false
            onBLEError.postValue(UIError(resources.getString(R.string.helium_pairing_failed_desc)) {
                setupBluetoothClaiming(macAddress)
            })
        }
    }

    fun connectToPeripheral() {
        viewModelScope.launch {
            connectionUseCase.connectToPeripheral().onLeft {
                analytics.trackEventFailure(it.code)
                when (it) {
                    is BluetoothError.BluetoothDisabledException -> {
                        onBLEError.postValue(
                            UIError(resources.getString(R.string.helium_bluetooth_disabled)) {
                                connectToPeripheral()
                            }
                        )
                    }
                    is BluetoothError.ConnectionLostException -> {
                        onBLEConnectionLost.postValue(true)
                    }
                    else -> {
                        onBLEError.postValue(
                            UIError(resources.getString(R.string.helium_pairing_failed_desc)) {
                                connectToPeripheral()
                            }
                        )
                    }
                }
            }.onRight {
                if (connectionUseCase.getPairedDevices().any {
                        it.address == selectedDeviceMacAddress
                    }
                ) {
                    onBLEConnection.postValue(true)
                }
            }
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

        viewModelScope.launch {
            connectionUseCase.registerOnBondStatus().collect {
                when (it) {
                    BluetoothDevice.BOND_BONDED -> {
                        onBLEConnection.postValue(true)
                    }
                    BluetoothDevice.BOND_NONE -> {
                        analytics.trackEventFailure(Failure.CODE_BL_DEVICE_NOT_PAIRED)
                        onBLEError.postValue(
                            UIError(resources.getString(R.string.helium_pairing_failed_desc)) {
                                connectToPeripheral()
                            }
                        )
                    }
                }
            }
        }
    }
}
