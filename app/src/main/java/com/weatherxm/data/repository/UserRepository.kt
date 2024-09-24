package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.User

interface UserRepository {
    suspend fun getUser(forceRefresh: Boolean = false): Either<Failure, User>
    suspend fun getUserUsername(): Either<Failure, String>
    suspend fun deleteAccount(): Either<Failure, Unit>
    fun getUserId(): String
}
