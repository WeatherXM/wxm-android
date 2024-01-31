package com.weatherxm.ui.devicesettings.reboot

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.data.BluetoothError
import com.weatherxm.data.Failure
import com.weatherxm.data.Resource
import com.weatherxm.ui.common.ScannedDevice
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.devicesettings.RebootState
import com.weatherxm.ui.devicesettings.RebootStatus
import com.weatherxm.usecases.BluetoothConnectionUseCase
import com.weatherxm.usecases.BluetoothScannerUseCase
import com.weatherxm.util.Analytics
import com.weatherxm.util.Failure.getCode
import com.weatherxm.util.Resources
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Suppress("TooManyFunctions")
class RebootViewModel(
    var device: UIDevice,
    private val resources: Resources,
    private val connectionUseCase: BluetoothConnectionUseCase,
    private val scanUseCase: BluetoothScannerUseCase,
    private val analytics: Analytics
) : ViewModel() {
    private val onStatus = MutableLiveData<Resource<RebootState>>()
    fun onStatus() = onStatus

    private var scannedDevice = ScannedDevice.empty()

    private fun deviceIsPaired(): Boolean {
        return connectionUseCase.getPairedDevices().any { it.address == scannedDevice.address }
    }

    val scanningJob: Job = viewModelScope.launch {
        scanUseCase.registerOnScanning().collect {
            @Suppress("MagicNumber")
            if (it.name?.contains(device.getLastCharsOfLabel(6)) == true) {
                scannedDevice = it
                scanUseCase.stopScanning()
            }
        }
    }

    private fun checkIfDevicePaired() {
        if (scannedDevice == ScannedDevice.empty()) {
            onStatus.postValue(
                Resource.error(
                    resources.getString(R.string.station_not_in_range_subtitle),
                    RebootState(RebootStatus.SCAN_FOR_STATION, BluetoothError.DeviceNotFound)
                )
            )
            return
        }

        if (deviceIsPaired()) {
            connectAndReboot()
        } else {
            analytics.trackEventFailure(Failure.CODE_BL_DEVICE_NOT_PAIRED)
            onStatus.postValue(
                Resource.error(
                    String.empty(), RebootState(RebootStatus.PAIR_STATION)
                )
            )
        }
    }

    @Suppress("MagicNumber")
    fun scanConnectAndReboot() {
        onStatus.postValue(Resource.loading(RebootState(RebootStatus.CONNECT_TO_STATION)))
        viewModelScope.launch {
            scanUseCase.startScanning().collect {
                it.onRight { progress ->
                    if (progress == 100) {
                        checkIfDevicePaired()
                    }
                }.onLeft { failure ->
                    analytics.trackEventFailure(failure.code)
                    onStatus.postValue(
                        Resource.error(String.empty(), RebootState(RebootStatus.SCAN_FOR_STATION))
                    )
                }
            }
        }
    }

    fun pairDevice() {
        viewModelScope.launch {
            onStatus.postValue(Resource.loading(RebootState(RebootStatus.CONNECT_TO_STATION)))
            connectionUseCase.setPeripheral(scannedDevice.address).onRight {
                connectionUseCase.connectToPeripheral().onRight {
                    if (deviceIsPaired()) {
                        reboot()
                    } else {
                        analytics.trackEventFailure(Failure.CODE_BL_DEVICE_NOT_PAIRED)
                        onStatus.postValue(
                            Resource.error(String.empty(), RebootState(RebootStatus.PAIR_STATION))
                        )
                    }
                }
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun disconnectFromPeripheral() {
        GlobalScope.launch {
            connectionUseCase.disconnectFromPeripheral()
        }
    }

    private fun connectAndReboot() {
        connectionUseCase.setPeripheral(scannedDevice.address).onRight {
            viewModelScope.launch {
                connectionUseCase.connectToPeripheral().onRight {
                    reboot()
                }.onLeft {
                    analytics.trackEventFailure(it.code)
                    onStatus.postValue(
                        Resource.error(it.getCode(), RebootState(RebootStatus.CONNECT_TO_STATION))
                    )
                }
            }
        }.onLeft {
            analytics.trackEventFailure(it.code)
            onStatus.postValue(
                Resource.error(it.getCode(), RebootState(RebootStatus.CONNECT_TO_STATION))
            )
        }
    }

    private fun reboot() {
        viewModelScope.launch {
            onStatus.postValue(Resource.loading(RebootState(RebootStatus.REBOOTING)))
            connectionUseCase.reboot().onRight {
                onStatus.postValue(Resource.success(RebootState(RebootStatus.REBOOTING)))
            }.onLeft {
                analytics.trackEventFailure(it.code)
                Resource.error(it.getCode(), RebootState(RebootStatus.REBOOTING))
            }
        }
    }

    init {
        viewModelScope.launch {
            connectionUseCase.registerOnBondStatus().collect {
                when (it) {
                    BluetoothDevice.BOND_BONDED -> {
                        reboot()
                    }
                    BluetoothDevice.BOND_NONE -> {
                        analytics.trackEventFailure(Failure.CODE_BL_DEVICE_NOT_PAIRED)
                        onStatus.postValue(
                            Resource.error(String.empty(), RebootState(RebootStatus.PAIR_STATION))
                        )
                    }
                }
            }
        }
    }
}
