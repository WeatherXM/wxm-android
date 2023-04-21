package com.weatherxm.ui.stationsettings.reboot

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.data.BluetoothError
import com.weatherxm.data.Device
import com.weatherxm.data.Resource
import com.weatherxm.ui.common.ScannedDevice
import com.weatherxm.ui.stationsettings.RebootState
import com.weatherxm.ui.stationsettings.RebootStatus
import com.weatherxm.usecases.BluetoothConnectionUseCase
import com.weatherxm.usecases.BluetoothScannerUseCase
import com.weatherxm.util.ResourcesHelper
import com.weatherxm.util.UIErrors.getCode
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Suppress("TooManyFunctions")
class RebootViewModel(var device: Device) : ViewModel(), KoinComponent {
    private val resHelper: ResourcesHelper by inject()
    private val connectionUseCase: BluetoothConnectionUseCase by inject()
    private val scanUseCase: BluetoothScannerUseCase by inject()

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
                    resHelper.getString(R.string.station_not_in_range_subtitle),
                    RebootState(RebootStatus.SCAN_FOR_STATION, BluetoothError.DeviceNotFound)
                )
            )
            return
        }

        if (deviceIsPaired()) {
            connectAndReboot()
        } else {
            onStatus.postValue(Resource.error("", RebootState(RebootStatus.PAIR_STATION)))
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
                }.onLeft {
                    onStatus.postValue(
                        Resource.error("", RebootState(RebootStatus.SCAN_FOR_STATION))
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
                        onStatus.postValue(
                            Resource.error("", RebootState(RebootStatus.PAIR_STATION))
                        )
                    }
                }
            }
        }
    }

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
                    onStatus.postValue(
                        Resource.error(it.getCode(), RebootState(RebootStatus.CONNECT_TO_STATION))
                    )
                }
            }
        }.onLeft {
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
                        onStatus.postValue(
                            Resource.error("", RebootState(RebootStatus.PAIR_STATION))
                        )
                    }
                }
            }
        }
    }
}