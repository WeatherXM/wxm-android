package com.weatherxm.ui.claimdevice.helium.pair

import android.bluetooth.BluetoothDevice
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.data.BluetoothError
import com.weatherxm.data.Resource
import com.weatherxm.data.Status
import com.weatherxm.data.datasource.bluetooth.BluetoothUpdaterDataSource
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

    // TODO: Remove on PR
    private val updater: BluetoothUpdaterDataSource by inject()

    private val onBLEError = MutableLiveData<UIError>()
    private val onBLEDevEUI = MutableLiveData<String>()
    private val onBLEClaimingKey = MutableLiveData<String>()
    private val onNewScannedDevice = MutableLiveData<List<ScannedDevice>>()
    private val onScanProgress = MutableLiveData<Resource<Unit>>()

    private var selectedDeviceMacAddress: String = ""

    fun onNewScannedDevice(): LiveData<List<ScannedDevice>> = onNewScannedDevice
    fun onScanProgress(): LiveData<Resource<Unit>> = onScanProgress
    fun onBLEError() = onBLEError
    fun onBLEDevEUI() = onBLEDevEUI
    fun onBLEClaimingKey() = onBLEClaimingKey

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
        scanDevicesUseCase.stopScanning()
        selectedDeviceMacAddress = macAddress
        connectionUseCase.setPeripheral(macAddress).tap {
            connectToPeripheral()
        }.tapLeft {
            onBLEError.postValue(UIError(resHelper.getString(R.string.helium_pairing_failed_desc)) {
                setupBluetoothClaiming(macAddress)
            })
        }
    }

    // TODO: Remove this on PR
    fun update(uri: Uri) {
        GlobalScope.launch {
            updater.setUpdater()
            updater.update(uri)
        }
    }

    private fun connectToPeripheral() {
        viewModelScope.launch {
            connectionUseCase.connectToPeripheral().tapLeft {
                onBLEError.postValue(when (it) {
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
                })
            }.tap {
                if (connectionUseCase.getPairedDevices()?.any {
                        it.address == selectedDeviceMacAddress
                    } == true
                ) {
                    fetchDeviceEUI()
                }
            }
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
