package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.datasource.AuthDataSource
import com.weatherxm.data.datasource.AuthTokenDataSource
import com.weatherxm.data.datasource.CacheUserDataSource
import com.weatherxm.data.datasource.CacheWalletDataSource
import com.weatherxm.data.datasource.CacheWeatherForecastDataSource
import com.weatherxm.data.datasource.CredentialsDataSource
import com.weatherxm.data.datasource.HttpCacheDataSource

interface AuthRepository {
    suspend fun login(username: String, password: String): Either<Failure, String>
    suspend fun logout()
    suspend fun isLoggedIn(): Either<Error, String>
    suspend fun signup(
        username: String,
        firstName: String?,
        lastName: String?
    ): Either<Failure, String>

    suspend fun resetPassword(email: String): Either<Failure, Unit>
}

@Suppress("LongParameterList")
class AuthRepositoryImpl(
    private val authTokenDatasource: AuthTokenDataSource,
    private val credentialsDatasource: CredentialsDataSource,
    private val authDataSource: AuthDataSource,
    private val userDataSource: CacheUserDataSource,
    private val walletDataSource: CacheWalletDataSource,
    private val weatherForecastDataSource: CacheWeatherForecastDataSource,
    private val httpCacheDataSource: HttpCacheDataSource
) : AuthRepository {

    override suspend fun login(username: String, password: String): Either<Failure, String> {
        return authDataSource.login(username, password)
    }

    override suspend fun logout() {
        authTokenDatasource.clear()
        credentialsDatasource.clear()
        userDataSource.clear()
        walletDataSource.clear()
        weatherForecastDataSource.clear()
        httpCacheDataSource.clear()
    }

    override suspend fun isLoggedIn(): Either<Error, String> {
        return credentialsDatasource.getCredentials().map { it.username }
    }

    override suspend fun signup(
        username: String,
        firstName: String?,
        lastName: String?
    ): Either<Failure, String> {
        return authDataSource.signup(username, firstName, lastName)
    }

    override suspend fun resetPassword(email: String): Either<Failure, Unit> {
        return authDataSource.resetPassword(email)
    }
}
