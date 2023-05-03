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
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DeviceHeliumOTAViewModel(
    val device: Device,
    val deviceIsBleConnected: Boolean
) : ViewModel(), KoinComponent {
    private val resHelper: ResourcesHelper by inject()
    private val connectionUseCase: BluetoothConnectionUseCase by inject()
    private val updaterUseCase: BluetoothUpdaterUseCase by inject()
    private val scanUseCase: BluetoothScannerUseCase by inject()

    private val onStatus = MutableLiveData<Resource<State>>()
    fun onStatus() = onStatus

    private val onInstallingProgress = MutableLiveData<Int>()
    fun onInstallingProgress() = onInstallingProgress

    private var scannedDevice = ScannedDevice.empty()

    private fun deviceIsPaired(): Boolean {
        return connectionUseCase.getPairedDevices().any { it.address == scannedDevice.address }
    }

    @Suppress("MagicNumber")
    fun startScan() {
        /**
         * If device is already connected we have no reason to BLE Scan again so we go directly
         * to downloading and installing.
         * But we want to launch this from here as when we get here it means that all necessary
         * checks on bluetooth are successful.
         */
        if (deviceIsBleConnected) {
            downloadFirmwareAndGetFileURI()
            return
        }
        onStatus.postValue(Resource.loading(State(OTAStatus.CONNECT_TO_STATION)))
        viewModelScope.launch {
            scanUseCase.startScanning().collect {
                it.onRight { progress ->
                    if (progress == 100) {
                        checkIfDevicePaired()
                    }
                }.onLeft {
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

        if (deviceIsPaired()) {
            setPeripheral()
        } else {
            onStatus.postValue(Resource.error("", State(OTAStatus.PAIR_STATION)))
        }
    }

    fun pairDevice() {
        viewModelScope.launch {
            connectionUseCase.setPeripheral(scannedDevice.address).onRight {
                connectionUseCase.connectToPeripheral().onRight {
                    if (deviceIsPaired()) {
                        downloadFirmwareAndGetFileURI()
                    } else {
                        onStatus.postValue(Resource.error("", State(OTAStatus.PAIR_STATION)))
                    }
                }
            }
        }
    }

    fun setPeripheral() {
        connectionUseCase.setPeripheral(scannedDevice.address).onRight {
            connectToPeripheral()
        }.onLeft {
            onStatus.postValue(Resource.error(it.getCode(), State(OTAStatus.CONNECT_TO_STATION)))
        }
    }

    private fun connectToPeripheral() {
        viewModelScope.launch {
            connectionUseCase.connectToPeripheral().onRight {
                downloadFirmwareAndGetFileURI()
            }.onLeft {
                onStatus.postValue(
                    Resource.error(it.getCode(), State(OTAStatus.CONNECT_TO_STATION))
                )
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun disconnectFromPeripheral() {
        GlobalScope.launch {
            connectionUseCase.disconnectFromPeripheral()
        }
    }

    private fun downloadFirmwareAndGetFileURI() {
        onStatus.postValue(Resource.loading(State(OTAStatus.DOWNLOADING)))
        viewModelScope.launch {
            updaterUseCase.downloadFirmwareAndGetFileURI(device.id).onRight {
                update(it)
            }.onLeft {
                onStatus.postValue(
                    Resource.error(
                        resHelper.getString(R.string.error_helium_ota_download_failed),
                        State(OTAStatus.DOWNLOADING)
                    )
                )
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun update(uri: Uri) {
        GlobalScope.launch {
            onStatus.postValue(Resource.loading(State(OTAStatus.INSTALLING)))
            updaterUseCase.update(uri).collect {
                when (it.state) {
                    BluetoothOTAState.IN_PROGRESS -> {
                        onInstallingProgress.postValue(it.progress)
                    }
                    BluetoothOTAState.COMPLETED -> {
                        onStatus.postValue(Resource.success(State(OTAStatus.INSTALLING)))
                        device.attributes?.firmware?.assigned?.let { versionInstalled ->
                            updaterUseCase.onUpdateSuccess(device.id, versionInstalled)
                        }
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
                @Suppress("MagicNumber")
                if (it.name?.contains(device.getLastCharsOfLabel(6)) == true) {
                    scannedDevice = it
                    scanUseCase.stopScanning()
                }
            }
        }

        viewModelScope.launch {
            connectionUseCase.registerOnBondStatus().collect {
                when (it) {
                    BluetoothDevice.BOND_BONDED -> {
                        downloadFirmwareAndGetFileURI()
                    }
                    BluetoothDevice.BOND_NONE -> {
                        onStatus.postValue(Resource.error("", State(OTAStatus.PAIR_STATION)))
                    }
                }
            }
        }
    }
}
