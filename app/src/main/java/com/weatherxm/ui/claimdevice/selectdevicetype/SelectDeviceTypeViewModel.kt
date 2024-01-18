package com.weatherxm.ui.claimdevice.selectdevicetype

import androidx.lifecycle.ViewModel
import com.weatherxm.ui.claimdevice.AvailableDeviceType
import com.weatherxm.usecases.SelectDeviceTypeUseCase

class SelectDeviceTypeViewModel(
    private val selectDeviceUseCase: SelectDeviceTypeUseCase
) : ViewModel() {

    fun getAvailableDeviceTypes(): List<AvailableDeviceType> {
        return selectDeviceUseCase.getAvailableDeviceTypes()
    }
}
