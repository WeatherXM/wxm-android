package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.ui.common.DevicesSortFilterOptions
import com.weatherxm.ui.common.UIDevice

interface DeviceListUseCase {
    suspend fun getUserDevices(): Either<Failure, List<UIDevice>>
    fun getDevicesSortFilterOptions(): DevicesSortFilterOptions
    fun setDevicesSortFilterOptions(options: DevicesSortFilterOptions)
}
