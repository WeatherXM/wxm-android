package com.weatherxm.data.datasource

import arrow.core.Either
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.User

interface UserDataSource {
    suspend fun getUserUsername(): Either<Failure, String>
    suspend fun setUserUsername(username: String)
    suspend fun getUser(): Either<Failure, User>
    suspend fun setUser(user: User)
    suspend fun deleteAccount(): Either<Failure, Unit>
    fun getUserId(): String
}
