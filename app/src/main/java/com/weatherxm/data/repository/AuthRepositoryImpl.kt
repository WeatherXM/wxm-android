package com.weatherxm.data.repository

import arrow.core.Either
import arrow.core.flatMap
import com.weatherxm.BuildConfig
import com.weatherxm.data.Failure
import com.weatherxm.data.datasource.CacheAuthDataSource
import com.weatherxm.data.datasource.CacheUserDataSource
import com.weatherxm.data.datasource.NetworkAuthDataSource
import com.weatherxm.data.network.AuthToken
import com.weatherxm.data.services.CacheService
import timber.log.Timber

@Suppress("LongParameterList")
class AuthRepositoryImpl(
    private val cacheAuthDataSource: CacheAuthDataSource,
    private val networkAuthDataSource: NetworkAuthDataSource,
    private val cacheUserDataSource: CacheUserDataSource,
    private val cacheService: CacheService
) : AuthRepository {

    override suspend fun login(username: String, password: String): Either<Failure, AuthToken> {
        return networkAuthDataSource.login(username, password).onRight {
            Timber.d("Login success. Saving username [username = $username].")
            cacheUserDataSource.setUserUsername(username)
        }
    }

    override suspend fun logout() {
        networkAuthDataSource.logout()
        cacheService.clearAll()
    }

    override suspend fun isLoggedIn(): Either<Failure, Boolean> {
        /**
         * Fix: Consider the user as logged in in local mock mode because otherwise this returns
         * Either.Left and some functionalities are limited because of a not logged in user
         */
        if (BuildConfig.FLAVOR_mode == "local") return Either.Right(true)

        return cacheAuthDataSource.getAuthToken().map {
            it.isAccessTokenValid() || it.isRefreshTokenValid()
        }
    }

    override suspend fun signup(
        username: String,
        firstName: String?,
        lastName: String?
    ): Either<Failure, Unit> {
        return networkAuthDataSource.signup(username, firstName, lastName).onRight {
            Timber.d("Signup success. Email sent to user: $username")
        }
    }

    override suspend fun resetPassword(email: String): Either<Failure, Unit> {
        return networkAuthDataSource.resetPassword(email)
    }

    override suspend fun isPasswordCorrect(password: String): Either<Failure, Boolean> {
        return cacheUserDataSource.getUserUsername().flatMap {
            networkAuthDataSource.login(it, password).map { true }
        }
    }
}
