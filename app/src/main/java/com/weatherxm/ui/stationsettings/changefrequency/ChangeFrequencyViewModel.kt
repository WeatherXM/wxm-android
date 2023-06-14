package com.weatherxm.ui.stationsettings.changefrequency

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.data.BluetoothError
import com.weatherxm.data.Device
import com.weatherxm.data.Failure
import com.weatherxm.data.Frequency
import com.weatherxm.data.Resource
import com.weatherxm.ui.common.FrequencyState
import com.weatherxm.ui.common.ScannedDevice
import com.weatherxm.ui.stationsettings.ChangeFrequencyState
import com.weatherxm.ui.stationsettings.FrequencyStatus
import com.weatherxm.usecases.BluetoothConnectionUseCase
import com.weatherxm.usecases.BluetoothScannerUseCase
import com.weatherxm.usecases.StationSettingsUseCase
import com.weatherxm.util.Analytics
import com.weatherxm.util.ResourcesHelper
import com.weatherxm.util.UIErrors.getCode
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Suppress("TooManyFunctions")
class ChangeFrequencyViewModel(var device: Device) : ViewModel(), KoinComponent {
    private val resHelper: ResourcesHelper by inject()
    private val usecase: StationSettingsUseCase by inject()
    private val connectionUseCase: BluetoothConnectionUseCase by inject()
    private val scanUseCase: BluetoothScannerUseCase by inject()
    private val analytics: Analytics by inject()

    private val frequenciesInOrder = mutableListOf<Frequency>()
    private var selectedFrequency = Frequency.US915

    private val onFrequencies = MutableLiveData<FrequencyState>()

    fun onFrequencies() = onFrequencies

    fun getSelectedFrequency(): String = selectedFrequency.toString()
    fun setSelectedFrequency(position: Int) {
        selectedFrequency = frequenciesInOrder[position]
    }

    private val onStatus = MutableLiveData<Resource<ChangeFrequencyState>>()
    fun onStatus() = onStatus

    private var scannedDevice = ScannedDevice.empty()

    fun getCountryAndFrequencies() {
        viewModelScope.launch {
            usecase.getCountryAndFrequencies(device.location?.lat, device.location?.lon).apply {
                frequenciesInOrder.add(recommendedFrequency)
                frequenciesInOrder.addAll(otherFrequencies)

                val recommendedLabel = country?.let {
                    "${recommendedFrequency.name} (${
                        resHelper.getString(R.string.recommended_frequency_for, it)
                    })"
                } ?: recommendedFrequency.name

                val frequencies = mutableListOf(recommendedLabel)
                frequencies.addAll(otherFrequencies.map { it.name })

                onFrequencies.postValue(FrequencyState(country, frequencies))
            }
        }
    }

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
                    ChangeFrequencyState(
                        FrequencyStatus.SCAN_FOR_STATION,
                        BluetoothError.DeviceNotFound
                    )
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
                    "",
                    ChangeFrequencyState(FrequencyStatus.PAIR_STATION)
                )
            )
        }
    }

    @Suppress("MagicNumber")
    fun scanConnectAndChangeFrequency() {
        onStatus.postValue(
            Resource.loading(ChangeFrequencyState(FrequencyStatus.CONNECT_TO_STATION))
        )
        viewModelScope.launch {
            scanUseCase.startScanning().collect {
                it.onRight { progress ->
                    if (progress == 100) {
                        checkIfDevicePaired()
                    }
                }.onLeft { failure ->
                    analytics.trackEventFailure(failure.code)
                    onStatus.postValue(
                        Resource.error("", ChangeFrequencyState(FrequencyStatus.SCAN_FOR_STATION))
                    )
                }
            }
        }
    }

    fun pairDevice() {
        viewModelScope.launch {
            onStatus.postValue(
                Resource.loading(ChangeFrequencyState(FrequencyStatus.CONNECT_TO_STATION))
            )
            connectionUseCase.setPeripheral(scannedDevice.address).onRight {
                connectionUseCase.connectToPeripheral().onRight {
                    if (deviceIsPaired()) {
                        changeFrequency()
                    } else {
                        analytics.trackEventFailure(Failure.CODE_BL_DEVICE_NOT_PAIRED)
                        onStatus.postValue(
                            Resource.error("", ChangeFrequencyState(FrequencyStatus.PAIR_STATION))
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
                    changeFrequency()
                }.onLeft {
                    analytics.trackEventFailure(it.code)
                    onStatus.postValue(
                        Resource.error(
                            it.getCode(),
                            ChangeFrequencyState(FrequencyStatus.CONNECT_TO_STATION)
                        )
                    )
                }
            }
        }.onLeft {
            analytics.trackEventFailure(it.code)
            onStatus.postValue(
                Resource.error(
                    it.getCode(),
                    ChangeFrequencyState(FrequencyStatus.CONNECT_TO_STATION)
                )
            )
        }
    }

    private fun changeFrequency() {
        viewModelScope.launch {
            onStatus.postValue(
                Resource.loading(ChangeFrequencyState(FrequencyStatus.CHANGING_FREQUENCY))
            )
            connectionUseCase.setFrequency(selectedFrequency).onRight {
                onStatus.postValue(
                    Resource.success(ChangeFrequencyState(FrequencyStatus.CHANGING_FREQUENCY))
                )
            }.onLeft {
                analytics.trackEventFailure(it.code)
                Resource.error(
                    it.getCode(), ChangeFrequencyState(FrequencyStatus.CHANGING_FREQUENCY)
                )
            }
        }
    }

    init {
        viewModelScope.launch {
            connectionUseCase.registerOnBondStatus().collect {
                when (it) {
                    BluetoothDevice.BOND_BONDED -> {
                        changeFrequency()
                    }
                    BluetoothDevice.BOND_NONE -> {
                        analytics.trackEventFailure(Failure.CODE_BL_DEVICE_NOT_PAIRED)
                        onStatus.postValue(
                            Resource.error("", ChangeFrequencyState(FrequencyStatus.PAIR_STATION))
                        )
                    }
                }
            }
        }
    }
}
