package com.weatherxm.ui.claimdevice.location

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.weatherxm.data.models.Location
import com.weatherxm.ui.common.DeviceType

class ClaimLocationViewModel : ViewModel() {
    private var installationLocation = Location(0.0, 0.0)
    private var deviceType = DeviceType.M5_WIFI

    private val onRequestUserLocation = MutableLiveData(false)

    fun onRequestUserLocation() = onRequestUserLocation

    fun requestUserLocation() {
        onRequestUserLocation.postValue(true)
    }

    fun setDeviceType(type: DeviceType) {
        deviceType = type
    }

    fun getDeviceType(): DeviceType {
        return deviceType
    }

    fun setInstallationLocation(lat: Double, lon: Double) {
        installationLocation.lat = lat
        installationLocation.lon = lon
    }

    fun getInstallationLocation(): Location {
        return installationLocation
    }
}
