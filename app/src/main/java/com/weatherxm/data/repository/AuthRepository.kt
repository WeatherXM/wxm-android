package com.weatherxm.data.repository

import arrow.core.Either
import com.haroldadmin.cnradapter.NetworkResponse
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
}

class AuthRepositoryImpl(
    private val authTokenDatasource: AuthTokenDataSource,
    private val credentialsDatasource: CredentialsDataSource,
) : AuthRepository, KoinComponent {

    private val service: AuthService by inject()

    override suspend fun login(username: String, password: String): Either<Error, String> {
        return when (val response = service.login(LoginBody(username, password))) {
            is NetworkResponse.Success -> {
                Timber.d("Login success. Saving credentials.")
                credentialsDatasource.setCredentials(Credentials(username, password))
                Either.Right(username)
            }
            is NetworkResponse.ServerError -> {
                Either.Left(
                    Error(
                        response.body?.message ?: response.error.message,
                        response.error
                    )
                )
            }
            is NetworkResponse.NetworkError -> {
                Either.Left(Error("Network Error", response.error))
            }
            is NetworkResponse.UnknownError -> {
                Either.Left(Error("Unknown Error", response.error))
            }
        }
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
        val registration = RegistrationBody(
            username = username,
            password = password,
            firstName = firstName,
            lastName = lastName
        )
        return when (val response = service.register(registration)) {
            is NetworkResponse.Success -> {
                Timber.d("Signup success. Saving credentials.")
                credentialsDatasource.setCredentials(Credentials(username, password))
                Either.Right(username)
            }
            is NetworkResponse.ServerError -> {
                Either.Left(
                    Error(
                        response.body?.message ?: response.error.message,
                        response.error
                    )
                )
            }
            is NetworkResponse.NetworkError -> {
                Either.Left(Error("Network Error", response.error))
            }
            is NetworkResponse.UnknownError -> {
                Either.Left(Error("Unknown Error", response.error))
            }
        }
    }
}
