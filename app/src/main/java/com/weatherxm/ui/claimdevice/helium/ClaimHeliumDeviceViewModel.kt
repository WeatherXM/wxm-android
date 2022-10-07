package com.weatherxm.ui.claimdevice.helium

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.koin.core.component.KoinComponent

class ClaimHeliumDeviceViewModel : ViewModel(), KoinComponent {
    private lateinit var devEUI: String
    private lateinit var deviceKey: String

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

    fun cancel() {
        onCancel.postValue(true)
    }

    fun next() {
        onNext.postValue(true)
    }
}
