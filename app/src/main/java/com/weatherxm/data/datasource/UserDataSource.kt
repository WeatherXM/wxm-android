package com.weatherxm.data.datasource

import android.content.SharedPreferences
import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.User
import com.weatherxm.data.map
import com.weatherxm.data.network.AddressBody
import com.weatherxm.data.network.ApiService

interface UserDataSource {
    suspend fun getUser(): Either<Failure, User>
    suspend fun saveAddress(address: String): Either<Failure, Unit>
    suspend fun clear()
}

class UserDataSourceImpl(
    private val apiService: ApiService,
    private val preferences: SharedPreferences
) : UserDataSource {

    override suspend fun getUser(): Either<Failure, User> {
        return apiService.getUser().map()
    }

    override suspend fun saveAddress(address: String): Either<Failure, Unit> {
        return apiService.saveAddress(AddressBody(address)).map()
    }

    override suspend fun clear() {
        preferences.edit().clear().apply()
    }
}
