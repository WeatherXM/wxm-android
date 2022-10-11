package com.weatherxm.usecases

import com.weatherxm.R
import com.weatherxm.ui.claimdevice.AvailableDeviceType
import com.weatherxm.ui.common.DeviceType
import com.weatherxm.util.ResourcesHelper
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface SelectDeviceTypeUseCase {
    fun getAvailableDeviceTypes(): List<AvailableDeviceType>
}

class SelectDeviceTypeUseCaseImpl : SelectDeviceTypeUseCase, KoinComponent {
    private val resHelper: ResourcesHelper by inject()

    override fun getAvailableDeviceTypes(): List<AvailableDeviceType> {
        return DeviceType.values().map {
            if (it == DeviceType.M5_WIFI) {
                AvailableDeviceType(
                    resHelper.getString(R.string.m5_ws1000_title),
                    resHelper.getString(R.string.m5_ws1000_desc),
                    DeviceType.M5_WIFI
                )
            } else {
                AvailableDeviceType(
                    resHelper.getString(R.string.helium_type_title),
                    resHelper.getString(R.string.helium_type_desc),
                    DeviceType.HELIUM
                )
            }
        }
    }
}
