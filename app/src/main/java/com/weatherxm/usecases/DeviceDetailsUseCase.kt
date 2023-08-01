package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.ui.common.RewardsInfo
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.UIForecast
import com.weatherxm.ui.explorer.UICell
import kotlinx.coroutines.flow.Flow

interface DeviceDetailsUseCase {
    suspend fun getUserDevices(): Either<Failure, List<UIDevice>>
    suspend fun getUserDevice(device: UIDevice): Either<Failure, UIDevice>
    suspend fun getTokenInfoLast30D(device: UIDevice): Either<Failure, RewardsInfo>
    fun getUnitPreferenceChangedFlow(): Flow<String>
    suspend fun getForecast(
        device: UIDevice,
        forceRefresh: Boolean = false
    ): Either<Failure, List<UIForecast>>

    suspend fun getAddressOfCell(cell: UICell): String?
}
