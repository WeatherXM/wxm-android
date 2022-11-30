package com.weatherxm.ui.deviceheliumota

import android.bluetooth.BluetoothDevice
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.data.BluetoothError
import com.weatherxm.data.BluetoothOTAState
import com.weatherxm.data.Device
import com.weatherxm.data.Resource
import com.weatherxm.ui.common.ScannedDevice
import com.weatherxm.usecases.BluetoothConnectionUseCase
import com.weatherxm.usecases.BluetoothScannerUseCase
import com.weatherxm.usecases.BluetoothUpdaterUseCase
import com.weatherxm.util.ResourcesHelper
import com.weatherxm.util.UIErrors.getCode
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DeviceHeliumOTAViewModel(
    val device: Device
) : ViewModel(), KoinComponent {
    private val resHelper: ResourcesHelper by inject()
    private val connectionUseCase: BluetoothConnectionUseCase by inject()
    private val updaterUseCase: BluetoothUpdaterUseCase by inject()
    private val scanUseCase: BluetoothScannerUseCase by inject()

    private val onStatus = MutableLiveData<Resource<State>>()
    fun onStatus() = onStatus

    private val onDownloadFile = MutableLiveData(false)
    fun onDownloadFile() = onDownloadFile

    private var scannedDevice = ScannedDevice.empty()

    @Suppress("MagicNumber")
    fun startScan() {
        onStatus.postValue(Resource.loading(State(OTAStatus.CONNECT_TO_STATION)))
        viewModelScope.launch {
            scanUseCase.startScanning().collect {
                it.tap { progress ->
                    if (progress == 100) {
                        checkIfDevicePaired()
                    }
                }.tapLeft {
                    onStatus.postValue(Resource.error("", State(OTAStatus.SCAN_FOR_STATION)))
                }
            }
        }
    }

    private fun checkIfDevicePaired() {
        if (scannedDevice == ScannedDevice.empty()) {
            onStatus.postValue(
                Resource.error(
                    resHelper.getString(R.string.station_not_in_range_subtitle),
                    State(OTAStatus.SCAN_FOR_STATION, BluetoothError.DeviceNotFound)
                )
            )
            return
        }

        if (connectionUseCase.getPairedDevices().any { it.address == scannedDevice.address }) {
            setPeripheral()
        } else {
            onStatus.postValue(Resource.error("", State(OTAStatus.PAIR_STATION)))
        }
    }

    fun setPeripheral() {
        connectionUseCase.setPeripheral(scannedDevice.address).tap {
            connectToPeripheral()
        }.tapLeft {
            onStatus.postValue(Resource.error(it.getCode(), State(OTAStatus.CONNECT_TO_STATION)))
        }
    }

    private fun connectToPeripheral() {
        viewModelScope.launch {
            connectionUseCase.connectToPeripheral().tap {
                downloadUpdate()
            }.tapLeft {
                onStatus.postValue(
                    Resource.error(it.getCode(), State(OTAStatus.CONNECT_TO_STATION))
                )
            }
        }
    }

    private fun downloadUpdate() {
        onStatus.postValue(Resource.loading(State(OTAStatus.DOWNLOADING)))
        viewModelScope.launch {
            // TODO: Download the update zip from the backend
            delay(2000L)
            onDownloadFile.postValue(true)
        }
    }

    fun update(uri: Uri) {
        GlobalScope.launch {
            onStatus.postValue(Resource.loading(State(OTAStatus.INSTALLING)))
            updaterUseCase.update(uri).collect {
                when (it.state) {
                    BluetoothOTAState.IN_PROGRESS -> {
                        // Show the progress as a percentage ???
                    }
                    BluetoothOTAState.COMPLETED -> {
                        onStatus.postValue(Resource.success(State(OTAStatus.INSTALLING)))
                    }
                    BluetoothOTAState.ABORTED -> {
                        onStatus.postValue(
                            Resource.error(
                                resHelper.getString(R.string.error_helium_ota_aborted),
                                State(OTAStatus.INSTALLING)
                            )
                        )
                    }
                    BluetoothOTAState.FAILED -> {
                        onStatus.postValue(
                            Resource.error(
                                "",
                                State(
                                    OTAStatus.INSTALLING,
                                    otaError = it.error,
                                    otaErrorType = it.errorType,
                                    otaErrorMessage = it.message
                                )
                            )
                        )
                    }
                }
            }
        }
    }

    init {
        viewModelScope.launch {
            scanUseCase.registerOnScanning().collect {
                // TODO: Check with Device's EUI
                if (it.name?.contains("40002F") == true) {
                    scannedDevice = it
                    scanUseCase.stopScanning()
                }
            }
        }

        viewModelScope.launch {
            connectionUseCase.registerOnBondStatus().collect {
                when (it) {
                    BluetoothDevice.BOND_BONDED -> {
                        downloadUpdate()
                    }
                    BluetoothDevice.BOND_NONE -> {
                        onStatus.postValue(Resource.error("", State(OTAStatus.PAIR_STATION)))
                    }
                }
            }
        }
    }
}
