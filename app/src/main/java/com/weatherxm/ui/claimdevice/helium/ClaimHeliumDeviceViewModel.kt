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
    private val onClaimResult = MutableLiveData<Resource<String>>().apply {
        value = Resource.loading()
    }

    fun onCancel() = onCancel
    fun onNext() = onNext
    fun onClaimResult() = onClaimResult

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

    fun cancel() {
        onCancel.postValue(true)
    }

    fun next() {
        onNext.postValue(true)
    }

    fun claimDevice() {
        onClaimResult.postValue(Resource.loading())
        viewModelScope.launch {
            // TODO: API call
            delay(3000L)
            onClaimResult.postValue(Resource.success("NEW WEATHER STATION"))
            delay(3000L)
            onClaimResult.postValue(Resource.error("Oopsie"))
        }
    }
}
