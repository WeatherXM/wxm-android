package com.weatherxm.ui.devicesettings.helium.changefrequency

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.BluetoothError
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.Frequency
import com.weatherxm.ui.common.Resource
import com.weatherxm.ui.common.FrequencyState
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.components.BluetoothHeliumViewModel
import com.weatherxm.ui.devicesettings.ChangeFrequencyState
import com.weatherxm.ui.devicesettings.FrequencyStatus
import com.weatherxm.usecases.BluetoothConnectionUseCase
import com.weatherxm.usecases.BluetoothScannerUseCase
import com.weatherxm.usecases.StationSettingsUseCase
import com.weatherxm.util.Failure.getCode
import com.weatherxm.util.Resources
import kotlinx.coroutines.launch

@Suppress("TooManyFunctions")
class ChangeFrequencyViewModel(
    val device: UIDevice,
    private val resources: Resources,
    private val usecase: StationSettingsUseCase,
    connectionUseCase: BluetoothConnectionUseCase,
    scanUseCase: BluetoothScannerUseCase,
    analytics: AnalyticsWrapper
) : BluetoothHeliumViewModel(
    device.getLastCharsOfLabel(),
    scanUseCase,
    connectionUseCase,
    analytics
) {
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

    fun getCountryAndFrequencies() {
        viewModelScope.launch {
            usecase.getCountryAndFrequencies(device.location?.lat, device.location?.lon).apply {
                frequenciesInOrder.add(recommendedFrequency)
                frequenciesInOrder.addAll(otherFrequencies)

                val recommendedLabel = country?.let {
                    "${recommendedFrequency.name} (${
                        resources.getString(R.string.recommended_frequency_for, it)
                    })"
                } ?: recommendedFrequency.name

                val frequencies = mutableListOf(recommendedLabel)
                frequencies.addAll(otherFrequencies.map { it.name })

                onFrequencies.postValue(FrequencyState(country, frequencies))
            }
        }
    }

    override fun onScanFailure(failure: Failure) {
        onStatus.postValue(
            if (failure == BluetoothError.DeviceNotFound) {
                Resource.error(
                    resources.getString(R.string.station_not_in_range_subtitle),
                    ChangeFrequencyState(
                        FrequencyStatus.SCAN_FOR_STATION, BluetoothError.DeviceNotFound
                    )
                )
            } else {
                Resource.error(
                    String.empty(), ChangeFrequencyState(FrequencyStatus.SCAN_FOR_STATION)
                )
            }
        )
    }

    override fun onNotPaired() {
        onStatus.postValue(
            Resource.error(String.empty(), ChangeFrequencyState(FrequencyStatus.PAIR_STATION))
        )
    }

    override fun onConnected() {
        changeFrequency()
    }

    override fun onConnectionFailure(failure: Failure) {
        onStatus.postValue(
            Resource.error(
                failure.getCode(), ChangeFrequencyState(FrequencyStatus.CONNECT_TO_STATION)
            )
        )
    }

    fun startConnectionProcess() {
        onStatus.postValue(
            Resource.loading(ChangeFrequencyState(FrequencyStatus.CONNECT_TO_STATION))
        )
        super.scanAndConnect()
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
                connectionUseCase.reboot()
            }.onLeft {
                analytics.trackEventFailure(it.code)
                Resource.error(
                    it.getCode(),
                    ChangeFrequencyState(FrequencyStatus.CHANGING_FREQUENCY)
                )
            }
        }
    }
}
