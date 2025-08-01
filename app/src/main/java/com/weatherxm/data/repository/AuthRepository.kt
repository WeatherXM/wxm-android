package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.data.models.Failure
import com.weatherxm.data.network.AuthToken

interface AuthRepository {
    suspend fun login(username: String, password: String): Either<Failure, AuthToken>
    suspend fun logout(): Either<Failure, Unit>
    fun isLoggedIn(): Boolean
    suspend fun signup(
        username: String,
        firstName: String?,
        lastName: String?
    ): Either<Failure, Unit>

    suspend fun resetPassword(email: String): Either<Failure, Unit>
    suspend fun isPasswordCorrect(password: String): Either<Failure, Boolean>
}
