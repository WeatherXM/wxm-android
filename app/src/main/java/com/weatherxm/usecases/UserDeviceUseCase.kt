package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.Device
import com.weatherxm.data.DeviceInfo
import com.weatherxm.data.Failure
import com.weatherxm.data.HourlyWeather
import com.weatherxm.ui.common.TokenInfo
import com.weatherxm.ui.common.UserDevice
import kotlinx.coroutines.flow.Flow

interface UserDeviceUseCase {
    suspend fun getUserDevices(): Either<Failure, List<UserDevice>>
    suspend fun getUserDevice(deviceId: String): Either<Failure, Device>
    suspend fun getTodayAndTomorrowForecast(
        device: Device,
        forceRefresh: Boolean = false
    ): Either<Failure, List<HourlyWeather>>

    suspend fun getTokenInfoLast30D(deviceId: String): Either<Failure, TokenInfo>
    fun getUnitPreferenceChangedFlow(): Flow<String>
}
