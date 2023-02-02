package com.weatherxm.ui.claimdevice.helium.pair

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.data.BluetoothError
import com.weatherxm.data.Resource
import com.weatherxm.ui.common.ScannedDevice
import com.weatherxm.ui.common.UIError
import com.weatherxm.ui.common.unmask
import com.weatherxm.usecases.BluetoothConnectionUseCase
import com.weatherxm.usecases.BluetoothScannerUseCase
import com.weatherxm.util.ResourcesHelper
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class ClaimHeliumPairViewModel : ViewModel(), KoinComponent {
    private val resHelper: ResourcesHelper by inject()
    private val scanDevicesUseCase: BluetoothScannerUseCase by inject()
    private val connectionUseCase: BluetoothConnectionUseCase by inject()
    private var scannedDevices: MutableList<ScannedDevice> = mutableListOf()

    private val onBLEError = MutableLiveData<UIError>()
    private val onBLEConnectionLost = MutableLiveData<Boolean>()
    private val onBLEDevEUI = MutableLiveData<String>()
    private val onBLEClaimingKey = MutableLiveData<String>()
    private val onNewScannedDevice = MutableLiveData<List<ScannedDevice>>()
    private val onScanStatus = MutableLiveData<Resource<Unit>>()
    private val onScanProgress = MutableLiveData<Int>()

    private var selectedDeviceMacAddress: String = ""
    private var bleFetchStarted = false

    fun onNewScannedDevice(): LiveData<List<ScannedDevice>> = onNewScannedDevice
    fun onScanStatus(): LiveData<Resource<Unit>> = onScanStatus
    fun onScanProgress(): LiveData<Int> = onScanProgress
    fun onBLEError() = onBLEError
    fun onBLEConnectionLost() = onBLEConnectionLost
    fun onBLEDevEUI() = onBLEDevEUI
    fun onBLEClaimingKey() = onBLEClaimingKey

    @Suppress("MagicNumber")
    fun scanBleDevices() {
        viewModelScope.launch {
            onScanStatus.postValue(Resource.loading())
            scannedDevices.clear()
            scanDevicesUseCase.startScanning().collect {
                it.tap { progress ->
                    if (progress == 100) {
                        onScanProgress.postValue(progress)
                        onScanStatus.postValue(Resource.success(Unit))
                    } else {
                        onScanProgress.postValue(progress)
                    }
                }.tapLeft {
                    onScanStatus.postValue(Resource.error(""))
                }
            }
        }
    }

    fun setupBluetoothClaiming(macAddress: String) {
        if (bleFetchStarted) return
        bleFetchStarted = true

        scanDevicesUseCase.stopScanning()
        selectedDeviceMacAddress = macAddress
        connectionUseCase.setPeripheral(macAddress).tap {
            connectToPeripheral()
        }.tapLeft {
            bleFetchStarted = false
            onBLEError.postValue(UIError(resHelper.getString(R.string.helium_pairing_failed_desc)) {
                setupBluetoothClaiming(macAddress)
            })
        }
    }

    fun connectToPeripheral() {
        viewModelScope.launch {
            connectionUseCase.connectToPeripheral().tapLeft {
                when (it) {
                    is BluetoothError.BluetoothDisabledException -> {
                        onBLEError.postValue(
                            UIError(resHelper.getString(R.string.helium_bluetooth_disabled)) {
                                connectToPeripheral()
                            }
                        )
                    }
                    is BluetoothError.ConnectionLostException -> {
                        onBLEConnectionLost.postValue(true)
                    }
                    else -> {
                        onBLEError.postValue(
                            UIError(resHelper.getString(R.string.helium_pairing_failed_desc)) {
                                connectToPeripheral()
                            }
                        )
                    }
                }
            }.tap {
                if (connectionUseCase.getPairedDevices().any {
                        it.address == selectedDeviceMacAddress
                    }
                ) {
                    fetchDeviceEUI()
                }
            }
        }
    }

    fun disconnectFromPeripheral() {
        GlobalScope.launch {
            connectionUseCase.disconnectFromPeripheral()
        }
    }

    private fun fetchDeviceEUI() {
        viewModelScope.launch {
            connectionUseCase.fetchDeviceEUI().tap {
                /**
                 * BLE returns Dev EUI with `:` in between so we need to unmask it
                 */
                onBLEDevEUI.postValue(it.unmask())
                fetchClaimingKey()
            }.tapLeft {
                onBLEError.postValue(UIError(
                    resHelper.getString(R.string.helium_fetching_info_failed)
                ) {
                    fetchDeviceEUI()
                })
            }
        }
    }

    private fun fetchClaimingKey() {
        viewModelScope.launch {
            connectionUseCase.fetchClaimingKey().tap {
                onBLEClaimingKey.postValue(it)
            }.tapLeft {
                onBLEError.postValue(UIError(
                    resHelper.getString(R.string.helium_fetching_info_failed)
                ) {
                    fetchClaimingKey()
                })
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
                        fetchDeviceEUI()
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
