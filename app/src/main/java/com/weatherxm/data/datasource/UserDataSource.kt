package com.weatherxm.data.datasource

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.User
import com.weatherxm.data.map
import com.weatherxm.data.network.AddressBody
import com.weatherxm.data.network.ApiService

interface UserDataSource {
    suspend fun getUser(): Either<Failure, User>
    suspend fun saveAddress(address: String): Either<Failure, Unit>
}

class UserDataSourceImpl(
    private val apiService: ApiService
) : UserDataSource {

    override suspend fun getUser(): Either<Failure, User> {
        return apiService.getUser().map()
    }

    override suspend fun saveAddress(address: String): Either<Failure, Unit> {
        return apiService.saveAddress(AddressBody(address)).map()
    }
}
