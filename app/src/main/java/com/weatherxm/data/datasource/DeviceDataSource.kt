package com.weatherxm.data.datasource

import arrow.core.Either
import com.haroldadmin.cnradapter.NetworkResponse
import com.weatherxm.data.Failure
import com.weatherxm.data.PublicDevice
import com.weatherxm.data.map
import com.weatherxm.data.network.ApiService

interface DeviceDataSource {
    suspend fun getPublicDevices(): Either<Failure, List<PublicDevice>>
}

class DeviceDataSourceImpl(
    private val apiService: ApiService
) : DeviceDataSource {

    override suspend fun getPublicDevices(): Either<Failure, List<PublicDevice>> {
        return apiService.getPublicDevices().map()
    }
}
