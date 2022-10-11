package com.weatherxm.ui.claimdevice.selectdevicetype

import androidx.lifecycle.ViewModel
import com.weatherxm.ui.claimdevice.AvailableDeviceType
import com.weatherxm.usecases.SelectDeviceTypeUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SelectDeviceTypeViewModel : ViewModel(), KoinComponent {
    private val selectDeviceUseCase: SelectDeviceTypeUseCase by inject()

    fun getAvailableDeviceTypes(): List<AvailableDeviceType> {
        return selectDeviceUseCase.getAvailableDeviceTypes()
    }
}
