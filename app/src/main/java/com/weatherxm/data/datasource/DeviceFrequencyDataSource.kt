package com.weatherxm.data.datasource

import arrow.core.Either
import com.weatherxm.data.mapResponse
import com.weatherxm.data.models.Failure
import com.weatherxm.data.network.ApiService
import com.weatherxm.data.network.DeviceFrequencyBody

interface DeviceFrequencyDataSource {
    suspend fun setDeviceFrequency(serialNumber: String, frequency: String): Either<Failure, Unit>
}

class DeviceFrequencyDataSourceImpl(
    private val apiService: ApiService
) : DeviceFrequencyDataSource {
    override suspend fun setDeviceFrequency(
        serialNumber: String,
        frequency: String
    ): Either<Failure, Unit> {
        return apiService.setDeviceFrequency(DeviceFrequencyBody(serialNumber, frequency))
            .mapResponse()
    }
}
