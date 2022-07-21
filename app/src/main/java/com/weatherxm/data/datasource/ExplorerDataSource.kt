package com.weatherxm.data.datasource

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.PublicDevice
import com.weatherxm.data.PublicHex
import com.weatherxm.data.map
import com.weatherxm.data.network.ApiService

interface ExplorerDataSource {
    suspend fun getPublicHexes(): Either<Failure, List<PublicHex>>
    suspend fun getPublicDevicesOfHex(index: String): Either<Failure, List<PublicDevice>>
    suspend fun getPublicDevice(index: String, deviceId: String): Either<Failure, PublicDevice>
}

class ExplorerDataSourceImpl(private val apiService: ApiService) : ExplorerDataSource {
    override suspend fun getPublicHexes(): Either<Failure, List<PublicHex>> {
        return apiService.getPublicHexes().map()
    }

    override suspend fun getPublicDevicesOfHex(index: String): Either<Failure, List<PublicDevice>> {
        return apiService.getPublicDevicesOfHex(index).map()
    }

    override suspend fun getPublicDevice(
        index: String,
        deviceId: String
    ): Either<Failure, PublicDevice> {
        return apiService.getPublicDevice(index, deviceId).map()
    }
}
