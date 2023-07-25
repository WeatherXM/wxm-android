package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.Device
import com.weatherxm.data.Failure
import com.weatherxm.ui.common.TokenInfo
import com.weatherxm.ui.common.UIForecast
import com.weatherxm.ui.common.UserDevice
import com.weatherxm.ui.explorer.UICell
import kotlinx.coroutines.flow.Flow

interface DeviceDetailsUseCase {
    suspend fun getUserDevices(): Either<Failure, List<UserDevice>>
    suspend fun getUserDevice(deviceId: String): Either<Failure, Device>
    suspend fun getTokenInfoLast30D(deviceId: String): Either<Failure, TokenInfo>
    fun getUnitPreferenceChangedFlow(): Flow<String>
    suspend fun getForecast(
        device: Device,
        forceRefresh: Boolean = false
    ): Either<Failure, List<UIForecast>>

    suspend fun getAddressOfCell(cell: UICell): String?
}
