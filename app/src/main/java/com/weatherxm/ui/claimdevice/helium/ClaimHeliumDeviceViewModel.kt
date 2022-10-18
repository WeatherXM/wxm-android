package com.weatherxm.ui.claimdevice.helium

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.koin.core.component.KoinComponent

class ClaimHeliumDeviceViewModel : ViewModel(), KoinComponent {
    private var devEUI: String = ""
    private var deviceKey: String = ""

    private val onCancel = MutableLiveData(false)
    private val onNext = MutableLiveData(false)

    fun onCancel() = onCancel
    fun onNext() = onNext

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
}
