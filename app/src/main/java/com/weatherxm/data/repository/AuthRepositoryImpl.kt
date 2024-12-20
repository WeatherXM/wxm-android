package com.weatherxm.data.repository

import arrow.core.Either
import arrow.core.flatMap
import com.weatherxm.BuildConfig
import com.weatherxm.data.datasource.AppConfigDataSource
import com.weatherxm.data.datasource.CacheAuthDataSource
import com.weatherxm.data.datasource.CacheUserDataSource
import com.weatherxm.data.datasource.DatabaseExplorerDataSource
import com.weatherxm.data.datasource.NetworkAuthDataSource
import com.weatherxm.data.models.Failure
import com.weatherxm.data.network.AuthToken
import com.weatherxm.data.services.CacheService
import timber.log.Timber

@Suppress("LongParameterList")
class AuthRepositoryImpl(
    private val cacheAuthDataSource: CacheAuthDataSource,
    private val networkAuthDataSource: NetworkAuthDataSource,
    private val cacheUserDataSource: CacheUserDataSource,
    private val databaseExplorerDataSource: DatabaseExplorerDataSource,
    private val appConfigDataSource: AppConfigDataSource,
    private val cacheService: CacheService
) : AuthRepository {

    override suspend fun login(username: String, password: String): Either<Failure, AuthToken> {
        return networkAuthDataSource.login(username, password).onRight {
            Timber.d("Login success. Saving username [username = $username].")
            cacheUserDataSource.setUserUsername(username)
        }
    }

    override suspend fun logout() {
        cacheService.getAuthToken().onRight {
            networkAuthDataSource.logout(it.access, appConfigDataSource.getInstallationId())
        }
        databaseExplorerDataSource.deleteAll()
        cacheService.clearAll()
    }

    override fun isLoggedIn(): Boolean {
        /**
         * Fix: Consider the user as logged in in local mock mode because otherwise this returns
         * Either.Left and some functionalities are limited because of a not logged in user
         */
        if (BuildConfig.FLAVOR_mode == "local") return true

        return cacheAuthDataSource.getAuthToken().fold(
            ifLeft = { false },
            ifRight = { it.isAccessTokenValid() || it.isRefreshTokenValid() }
        )
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
