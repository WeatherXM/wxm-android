package com.weatherxm.data.repository

import arrow.core.Either
import com.haroldadmin.cnradapter.NetworkResponse
import com.weatherxm.data.User
import com.weatherxm.data.network.ApiService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class UserRepository : KoinComponent {

    private val apiService: ApiService by inject()

    suspend fun getUser(): Either<Error, User> {
        return when (val response = apiService.user()) {
            is NetworkResponse.Success -> {
                val user = response.body
                Timber.d("Got user: ${user.email} [${user.authority}]")
                return Either.Right(user)
            }
            is NetworkResponse.ServerError -> {
                Either.Left(Error("Failed to get user with Server Error"))
            }
            is NetworkResponse.NetworkError -> {
                Either.Left(Error("Failed to get user with Network Error"))
            }
            is NetworkResponse.UnknownError -> {
                Either.Left(Error("Failed to get user with Unknown Error"))
            }
        }
    }
}
