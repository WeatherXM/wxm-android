package com.weatherxm.ui.claimdevice.helium

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.koin.core.component.KoinComponent

class ClaimHeliumDeviceViewModel : ViewModel(), KoinComponent {
    private lateinit var devEUI: String
    private lateinit var deviceKey: String
    private var deviceAddress: String = ""

    private var isManual = false

    private val onCancel = MutableLiveData(false)
    private val onNext = MutableLiveData(false)

    fun onCancel() = onCancel
    fun onNext() = onNext

    fun setup(isManual: Boolean?, macAddress: String?) {
        this.isManual = isManual == true
        macAddress?.let {
            deviceAddress = it
        }
    }

    fun setDeviceEUI(devEUI: String) {
        this.devEUI = devEUI
    }

    fun setDeviceKey(key: String) {
        deviceKey = key
    }

    fun getDeviceAddress(): String {
        return deviceAddress
    }

    fun isManualClaiming(): Boolean {
        return isManual
    }

    fun cancel() {
        onCancel.postValue(true)
    }

    fun next() {
        onNext.postValue(true)
    }
}
