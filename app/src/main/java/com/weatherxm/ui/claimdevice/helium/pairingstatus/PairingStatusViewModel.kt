package com.weatherxm.ui.claimdevice.helium.pairingstatus

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.data.Resource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

class PairingStatusViewModel : ViewModel(), KoinComponent {
    private val onPairing = MutableLiveData<Resource<String>>()

    fun onPairing() = onPairing

    fun pair() {
        viewModelScope.launch {
            // TODO: Actual API call
            onPairing.postValue(Resource.loading())
            delay(3000L)
            onPairing.postValue(Resource.error("Oopsie"))
            delay(3000L)
            onPairing.postValue(Resource.success(null))
        }
    }
}
