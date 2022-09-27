package com.weatherxm.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.weatherxm.ui.ScannedDevice
import org.koin.core.component.KoinComponent

class HomeViewModel : ViewModel(), KoinComponent {
    private val onScannedDeviceSelected = MutableLiveData<ScannedDevice>()
    fun onScannedDeviceSelected(): LiveData<ScannedDevice> = onScannedDeviceSelected

    private val onClaimM5Manually = MutableLiveData(false)
    fun onClaimM5Manually() = onClaimM5Manually

    private val onClaimHeliumManually = MutableLiveData(false)
    fun onClaimHeliumManually() = onClaimHeliumManually

    private val onScanDevices = MutableLiveData(false)
    fun onScanDevices() = onScanDevices

    fun scanDevices() {
        onScanDevices.postValue(true)
    }

    fun selectScannedDevice(scannedDevice: ScannedDevice) {
        onScannedDeviceSelected.postValue(scannedDevice)
    }

    fun claimM5Manually() {
        onClaimM5Manually.postValue(true)
    }

    fun claimHeliumManually() {
        onClaimHeliumManually.postValue(true)
    }
}
