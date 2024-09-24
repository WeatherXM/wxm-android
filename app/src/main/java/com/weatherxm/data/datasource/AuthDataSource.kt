package com.weatherxm.data.datasource

import arrow.core.Either
import com.weatherxm.data.models.Failure
import com.weatherxm.data.network.AuthToken

interface AuthDataSource {
    suspend fun resetPassword(
        email: String
    ): Either<Failure, Unit>

    suspend fun login(
        username: String,
        password: String
    ): Either<Failure, AuthToken>

    suspend fun signup(
        username: String,
        firstName: String?,
        lastName: String?
    ): Either<Failure, Unit>

    suspend fun logout(
        accessToken: String,
        installationId: String? = null
    ): Either<Failure, Unit>

    suspend fun refresh(authToken: AuthToken): Either<Failure, AuthToken>
    suspend fun getAuthToken(): Either<Failure, AuthToken>
    suspend fun setAuthToken(token: AuthToken)
}
