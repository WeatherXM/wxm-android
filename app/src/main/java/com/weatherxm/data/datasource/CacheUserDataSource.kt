package com.weatherxm.data.datasource

import arrow.core.Either
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.User
import com.weatherxm.data.services.CacheService

class CacheUserDataSource(private val cacheService: CacheService) : UserDataSource {

    override suspend fun getUserUsername(): Either<Failure, String> {
        return cacheService.getUserUsername()
    }

    override suspend fun setUserUsername(username: String) {
        cacheService.setUserUsername(username)
    }

    override fun getUserId(): String {
        return cacheService.getUserId()
    }

    override suspend fun getUser(): Either<Failure, User> {
        return cacheService.getUser()
    }

    override suspend fun setUser(user: User) {
        cacheService.setUser(user)
    }

    override suspend fun deleteAccount(): Either<Failure, Unit> {
        throw NotImplementedError("Won't be implemented. Ignore this.")
    }
}
