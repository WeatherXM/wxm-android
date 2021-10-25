package com.weatherxm.data.repository

import arrow.core.Either
import com.haroldadmin.cnradapter.NetworkResponse
import com.weatherxm.data.network.ApiService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.squareup.moshi.Moshi
import com.weatherxm.data.Device
import com.weatherxm.data.Failure
import com.weatherxm.data.PublicDevice
import com.weatherxm.data.datasource.DeviceDataSource

class DeviceRepository(
    private val deviceDataSource: DeviceDataSource
) : KoinComponent {

    private val apiService: ApiService by inject()
    private val moshi: Moshi by inject()

    suspend fun getCustomerDevices(customerId: String): Either<Error, List<Device>> {
        return when (val response = apiService.getCustomerDevices(customerId)) {
            is NetworkResponse.Success -> {
                return Either.Right(response.body.data)
            }
            is NetworkResponse.ServerError -> {
                Either.Left(Error("Failed to get customer devices with Server Error"))
            }
            is NetworkResponse.NetworkError -> {
                Either.Left(Error("Failed to get customer devices with Network Error"))
            }
            is NetworkResponse.UnknownError -> {
                Either.Left(Error("Failed to get customer devices with Unknown Error"))
            }
        }
    }

    suspend fun getPublicDevices(): Either<Failure, List<PublicDevice>> {
        return deviceDataSource.getPublicDevices()
    }

    fun getNameOrLabel(name: String, label: String?): Either<Failure, String> {
        return if (!label.isNullOrEmpty()) {
            Either.Right("$label ($name)")
        } else {
            Either.Right(name)
        }
    }
}
