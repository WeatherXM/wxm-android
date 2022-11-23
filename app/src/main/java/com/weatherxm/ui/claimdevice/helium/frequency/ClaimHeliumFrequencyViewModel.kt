package com.weatherxm.ui.claimdevice.helium.frequency

import android.location.Location
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.data.Frequency
import com.weatherxm.usecases.ClaimDeviceUseCase
import com.weatherxm.util.ResourcesHelper
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ClaimHeliumFrequencyViewModel : ViewModel(), KoinComponent {
    private val resHelper: ResourcesHelper by inject()
    private val usecase: ClaimDeviceUseCase by inject()

    private val frequenciesInOrder = mutableListOf<Frequency>()
    private var selectedFrequency: Frequency? = null

    private val onFrequencyState = MutableLiveData<FrequencyState>()
    fun onFrequencyState() = onFrequencyState

    fun selectFrequency(position: Int) {
        selectedFrequency = frequenciesInOrder[position]
    }

    fun getSelectedFrequency(): Frequency? {
        return selectedFrequency
    }

    fun getCountryAndFrequencies(location: Location) {
        viewModelScope.launch {
            usecase.getCountryAndFrequencies(location).apply {
                frequenciesInOrder.add(recommendedFrequency)
                frequenciesInOrder.addAll(otherFrequencies)

                val recommendedLabel = country?.let {
                    "${recommendedFrequency.name} (${
                        resHelper.getString(R.string.recommended_frequency_for, it)
                    })"
                } ?: recommendedFrequency.name

                val frequencies = mutableListOf(recommendedLabel)
                frequencies.addAll(otherFrequencies.map { it.name })

                onFrequencyState.postValue(FrequencyState(country, frequencies))
            }
        }
    }
}
