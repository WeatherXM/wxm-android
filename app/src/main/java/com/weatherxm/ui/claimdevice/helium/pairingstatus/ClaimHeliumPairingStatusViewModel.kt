package com.weatherxm.ui.claimdevice.helium.pairingstatus

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.data.Resource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

class ClaimHeliumPairingStatusViewModel : ViewModel(), KoinComponent {
    private val onPairing = MutableLiveData<Resource<String>>().apply {
        value = Resource.loading()
    }

    fun onPairing() = onPairing

    fun pair(devEUI: String, deviceKey: String) {
        viewModelScope.launch {
            // TODO: Actual API call
            delay(5000L)
            onPairing.postValue(Resource.success(null))
        }
    }
}
