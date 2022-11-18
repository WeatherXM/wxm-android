package com.weatherxm.data.datasource

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.network.AuthToken
import com.weatherxm.data.services.CacheService

class CacheAuthDataSource(private val cacheService: CacheService) : AuthDataSource {

    override suspend fun getAuthToken(): Either<Failure, AuthToken> {
        return cacheService.getAuthToken()
    }

    override suspend fun setAuthToken(token: AuthToken) {
        cacheService.setAuthToken(token)
    }

    override suspend fun resetPassword(email: String): Either<Failure, Unit> {
        throw NotImplementedError("Won't be implemented. Ignore this.")
    }

    override suspend fun login(username: String, password: String): Either<Failure, AuthToken> {
        throw NotImplementedError("Won't be implemented. Ignore this.")
    }

    override suspend fun signup(
        username: String,
        firstName: String?,
        lastName: String?
    ): Either<Failure, Unit> {
        throw NotImplementedError("Won't be implemented. Ignore this.")
    }

    override suspend fun logout(): Either<Failure, Unit> {
        throw NotImplementedError("Won't be implemented. Ignore this.")
    }

    override suspend fun refresh(authToken: AuthToken): Either<Failure, AuthToken> {
        throw NotImplementedError("Won't be implemented. Ignore this.")
    }
}
