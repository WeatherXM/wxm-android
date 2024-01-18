package com.weatherxm.usecases

import com.weatherxm.R
import com.weatherxm.ui.claimdevice.AvailableDeviceType
import com.weatherxm.ui.common.DeviceType
import com.weatherxm.util.Resources

interface SelectDeviceTypeUseCase {
    fun getAvailableDeviceTypes(): List<AvailableDeviceType>
}

class SelectDeviceTypeUseCaseImpl(private val resources: Resources) : SelectDeviceTypeUseCase {

    override fun getAvailableDeviceTypes(): List<AvailableDeviceType> {
        return DeviceType.values().map {
            if (it == DeviceType.M5_WIFI) {
                AvailableDeviceType(
                    resources.getString(R.string.m5_ws1000_title),
                    resources.getString(R.string.m5_ws1000_desc),
                    DeviceType.M5_WIFI
                )
            } else {
                AvailableDeviceType(
                    resources.getString(R.string.helium_type_title),
                    resources.getString(R.string.helium_type_desc),
                    DeviceType.HELIUM
                )
            }
        }
    }
}
