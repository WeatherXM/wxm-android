package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.CountryAndFrequencies
import com.weatherxm.data.DeviceInfo
import com.weatherxm.data.Failure
import com.weatherxm.ui.common.UIDevice

interface StationSettingsUseCase {
    suspend fun setFriendlyName(deviceId: String, friendlyName: String): Either<Failure, Unit>
    suspend fun clearFriendlyName(deviceId: String): Either<Failure, Unit>
    suspend fun removeDevice(serialNumber: String, id: String): Either<Failure, Unit>
    fun userShouldNotifiedOfOTA(device: UIDevice): Boolean
    suspend fun getCountryAndFrequencies(lat: Double?, lon: Double?): CountryAndFrequencies
    suspend fun getDeviceInfo(deviceId: String): Either<Failure, DeviceInfo>
}
