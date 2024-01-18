package com.weatherxm.ui.deviceheliumota

import android.bluetooth.BluetoothDevice
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.data.BluetoothError
import com.weatherxm.data.BluetoothOTAState
import com.weatherxm.data.Failure
import com.weatherxm.data.Resource
import com.weatherxm.ui.common.ScannedDevice
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.usecases.BluetoothConnectionUseCase
import com.weatherxm.usecases.BluetoothScannerUseCase
import com.weatherxm.usecases.BluetoothUpdaterUseCase
import com.weatherxm.util.Analytics
import com.weatherxm.util.Failure.getCode
import com.weatherxm.util.Resources
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@Suppress("LongParameterList")
class DeviceHeliumOTAViewModel(
    val device: UIDevice,
    val deviceIsBleConnected: Boolean,
    private val resources: Resources,
    private val connectionUseCase: BluetoothConnectionUseCase,
    private val updaterUseCase: BluetoothUpdaterUseCase,
    private val scanUseCase: BluetoothScannerUseCase,
    private val analytics: Analytics
) : ViewModel() {
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
                }.onLeft { failure ->
                    analytics.trackEventFailure(failure.code)
                    onStatus.postValue(Resource.error("", State(OTAStatus.SCAN_FOR_STATION)))
                }
            }
        }
    }

    private fun checkIfDevicePaired() {
        if (scannedDevice == ScannedDevice.empty()) {
            onStatus.postValue(
                Resource.error(
                    resources.getString(R.string.station_not_in_range_subtitle),
                    State(OTAStatus.SCAN_FOR_STATION, BluetoothError.DeviceNotFound)
                )
            )
            return
        }

        if (deviceIsPaired()) {
            setPeripheral()
        } else {
            analytics.trackEventFailure(Failure.CODE_BL_DEVICE_NOT_PAIRED)
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
                }.onLeft {
                    analytics.trackEventFailure(it.code)
                }
            }.onLeft {
                analytics.trackEventFailure(it.code)
            }
        }
    }

    fun setPeripheral() {
        connectionUseCase.setPeripheral(scannedDevice.address).onRight {
            connectToPeripheral()
        }.onLeft {
            analytics.trackEventFailure(it.code)
            onStatus.postValue(Resource.error(it.getCode(), State(OTAStatus.CONNECT_TO_STATION)))
        }
    }

    private fun connectToPeripheral() {
        viewModelScope.launch {
            connectionUseCase.connectToPeripheral().onRight {
                downloadFirmwareAndGetFileURI()
            }.onLeft {
                analytics.trackEventFailure(it.code)
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
                analytics.trackEventFailure(it.code)
                onStatus.postValue(
                    Resource.error(
                        resources.getString(R.string.error_helium_ota_download_failed),
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
                        device.assignedFirmware?.let { versionInstalled ->
                            updaterUseCase.onUpdateSuccess(device.id, versionInstalled)
                        }
                    }
                    BluetoothOTAState.ABORTED -> {
                        onStatus.postValue(
                            Resource.error(
                                resources.getString(R.string.error_helium_ota_aborted),
                                State(OTAStatus.INSTALLING)
                            )
                        )
                    }
                    BluetoothOTAState.FAILED -> {
                        analytics.trackEventFailure(Failure.CODE_BL_OTA_FAILED)
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
                        analytics.trackEventFailure(Failure.CODE_BL_DEVICE_NOT_PAIRED)
                        onStatus.postValue(Resource.error("", State(OTAStatus.PAIR_STATION)))
                    }
                }
            }
        }
    }
}
