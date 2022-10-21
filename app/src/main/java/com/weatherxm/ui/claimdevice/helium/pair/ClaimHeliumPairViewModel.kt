package com.weatherxm.ui.claimdevice.helium.pair

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.data.BluetoothError
import com.weatherxm.data.Resource
import com.weatherxm.data.Status
import com.weatherxm.ui.common.ScannedDevice
import com.weatherxm.ui.common.UIError
import com.weatherxm.usecases.BluetoothConnectionUseCase
import com.weatherxm.usecases.BluetoothScannerUseCase
import com.weatherxm.util.ResourcesHelper
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class ClaimHeliumPairViewModel : ViewModel(), KoinComponent {
    private val resHelper: ResourcesHelper by inject()
    private val scanDevicesUseCase: BluetoothScannerUseCase by inject()
    private val bluetoothConnectionUseCase: BluetoothConnectionUseCase by inject()
    private var scannedDevices: MutableList<ScannedDevice> = mutableListOf()

    private val onBLEPaired = MutableLiveData(false)
    private val onBLEError = MutableLiveData<UIError>()
    private val onBLEDevEUI = MutableLiveData<String>()
    private val onNewScannedDevice = MutableLiveData<List<ScannedDevice>>()
    private val onScanProgress = MutableLiveData<Resource<Unit>>()

    fun onNewScannedDevice(): LiveData<List<ScannedDevice>> = onNewScannedDevice
    fun onScanProgress(): LiveData<Resource<Unit>> = onScanProgress
    fun onBLEPaired() = onBLEPaired
    fun onBLEError() = onBLEError
    fun onBLEDevEUI() = onBLEDevEUI

    fun scanBleDevices() {
        viewModelScope.launch {
            onScanProgress.postValue(Resource.loading())
            scannedDevices.clear()
            scanDevicesUseCase.startScanning().tap {
                onScanProgress.postValue(Resource.success(Unit))
            }.tapLeft {
                onScanProgress.postValue(Resource.error(""))
            }
        }
    }

    fun isScanningRunning(): Boolean {
        return onScanProgress.value?.status == Status.LOADING
    }

    fun setupBluetoothClaiming(macAddress: String) {
        bluetoothConnectionUseCase.getDeviceEUI(macAddress).tap {
            onBLEDevEUI.postValue(it)
            setPeripheral(macAddress)
        }.tapLeft {
            onBLEError.postValue(
                UIError(resHelper.getString(R.string.helium_pairing_failed_desc)) {
                    setupBluetoothClaiming(macAddress)
                })
        }
    }

    private fun setPeripheral(macAddress: String) {
        bluetoothConnectionUseCase.setPeripheral(macAddress).tap {
            connectToPeripheral()
        }.tapLeft {
            onBLEError.postValue(UIError(resHelper.getString(R.string.helium_pairing_failed_desc)) {
                setPeripheral(macAddress)
            })
        }
    }

    private fun connectToPeripheral() {
        viewModelScope.launch {
            bluetoothConnectionUseCase.connectToPeripheral().mapLeft {
                onBLEError.postValue(
                    when (it) {
                        is BluetoothError.BluetoothDisabledException -> {
                            UIError(resHelper.getString(R.string.helium_bluetooth_disabled)) {
                                connectToPeripheral()
                            }
                        }
                        is BluetoothError.ConnectionLostException -> {
                            UIError(resHelper.getString(R.string.helium_bluetooth_connection_lost)) {
                                connectToPeripheral()
                            }
                        }
                        else -> {
                            UIError(resHelper.getString(R.string.helium_pairing_failed_desc)) {
                                connectToPeripheral()
                            }
                        }
                    }
                )
            }
        }
    }

    init {
        viewModelScope.launch {
            scanDevicesUseCase.registerOnScanning()
                .collect {
                    if (!scannedDevices.contains(it)) {
                        Timber.d("New scanned device collected: $it")
                        scannedDevices.add(it)
                        this@ClaimHeliumPairViewModel.onNewScannedDevice.postValue(scannedDevices)
                    }
                }
        }

        viewModelScope.launch {
            bluetoothConnectionUseCase.registerOnBondStatus().collect {
                when (it) {
                    BluetoothDevice.BOND_BONDED -> {
                        onBLEPaired.postValue(true)
                    }
                    BluetoothDevice.BOND_BONDING -> {
                        onBLEPaired.postValue(false)
                    }
                    BluetoothDevice.BOND_NONE -> {
                        onBLEError.postValue(
                            UIError(resHelper.getString(R.string.helium_pairing_failed_desc)) {
                                connectToPeripheral()
                            }
                        )
                    }
                }
            }
        }
    }
}
