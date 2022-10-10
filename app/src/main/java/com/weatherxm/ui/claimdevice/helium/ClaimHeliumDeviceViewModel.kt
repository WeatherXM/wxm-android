package com.weatherxm.ui.claimdevice.helium

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.data.Resource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

class ClaimHeliumDeviceViewModel : ViewModel(), KoinComponent {
    private var devEUI: String = ""
    private var deviceKey: String = ""

    private val onCancel = MutableLiveData(false)
    private val onNext = MutableLiveData(false)
    private val onPairing = MutableLiveData<Resource<String>>()

    fun onCancel() = onCancel
    fun onNext() = onNext
    fun onPairing() = onPairing

    fun setDeviceEUI(devEUI: String) {
        this.devEUI = devEUI
    }

    fun setDeviceKey(key: String) {
        deviceKey = key
    }

    fun getDevEUI(): String {
        return devEUI
    }

    fun getDeviceKey(): String {
        return deviceKey
    }

    fun resetAndPair() {
        viewModelScope.launch {
            // TODO: Actual API call
            onPairing.postValue(Resource.loading())
            delay(3000L)
            onPairing.postValue(Resource.error("Oopsie"))
            delay(3000L)
            onPairing.postValue(Resource.success(null))
        }
    }

    fun cancel() {
        onCancel.postValue(true)
    }

    fun next() {
        onNext.postValue(true)
    }
}
