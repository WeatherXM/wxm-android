package com.weatherxm.ui.claimdevice.helium.frequency

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.data.models.Frequency
import com.weatherxm.data.models.Location
import com.weatherxm.ui.common.FrequencyState
import com.weatherxm.usecases.ClaimDeviceUseCase
import com.weatherxm.util.Resources
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ClaimHeliumFrequencyViewModel(
    private val usecase: ClaimDeviceUseCase,
    private val resources: Resources,
    private val dispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val frequenciesInOrder = mutableListOf<Frequency>()

    private val onFrequencyState = MutableLiveData<FrequencyState>()

    fun onFrequencyState() = onFrequencyState

    fun getFrequency(position: Int): Frequency {
        return frequenciesInOrder[position]
    }

    fun getCountryAndFrequencies(location: Location) {
        viewModelScope.launch(dispatcher) {
            usecase.getCountryAndFrequencies(location).apply {
                frequenciesInOrder.add(recommendedFrequency)
                frequenciesInOrder.addAll(otherFrequencies)

                val recommendedLabel = country?.let {
                    "${recommendedFrequency.name} (${
                        resources.getString(R.string.recommended_frequency_for, it)
                    })"
                } ?: recommendedFrequency.name

                val frequencies = mutableListOf(recommendedLabel)
                frequencies.addAll(otherFrequencies.map { it.name })

                onFrequencyState.postValue(FrequencyState(country, frequencies))
            }
        }
    }
}
