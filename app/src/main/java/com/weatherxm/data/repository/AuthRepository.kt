package com.weatherxm.data.repository

import arrow.core.Either
import com.haroldadmin.cnradapter.NetworkResponse
import com.weatherxm.data.Failure
import com.weatherxm.data.datasource.AuthDataSource
import com.weatherxm.data.datasource.AuthTokenDataSource
import com.weatherxm.data.datasource.CredentialsDataSource
import com.weatherxm.data.network.AuthService
import com.weatherxm.data.network.Credentials
import com.weatherxm.data.network.LoginBody
import com.weatherxm.data.network.RegistrationBody
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

interface AuthRepository {
    suspend fun login(username: String, password: String): Either<Error, String>
    suspend fun logout()
    suspend fun isLoggedIn(): Either<Error, String>
    suspend fun signup(
        username: String,
        password: String,
        firstName: String?,
        lastName: String?
    ): Either<Error, String>
    suspend fun resetPassword(email: String): Either<Failure,Unit>
}

class AuthRepositoryImpl(
    private val authTokenDatasource: AuthTokenDataSource,
    private val credentialsDatasource: CredentialsDataSource,
    private val authDataSource: AuthDataSource
) : AuthRepository, KoinComponent {

    override suspend fun login(username: String, password: String): Either<Error, String> {
        return authDataSource.login(username, password)
    }

    override suspend fun logout() {
        authTokenDatasource.clear()
        credentialsDatasource.clear()
    }

    override suspend fun isLoggedIn(): Either<Error, String> {
        return credentialsDatasource.getCredentials().map { it.username }
    }

    override suspend fun signup(
        username: String,
        password: String,
        firstName: String?,
        lastName: String?
    ): Either<Error, String> {
        return authDataSource.signup(username, password, firstName, lastName)
    }

    override suspend fun resetPassword(email: String): Either<Failure, Unit> {
        return authDataSource.resetPassword(email)
    }
}
