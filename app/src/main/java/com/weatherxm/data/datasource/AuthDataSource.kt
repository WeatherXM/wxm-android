package com.weatherxm.data.datasource

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.map
import com.weatherxm.data.network.ApiService
import com.weatherxm.data.network.AuthService
import com.weatherxm.data.network.Credentials
import com.weatherxm.data.network.LoginBody
import com.weatherxm.data.network.RegistrationBody
import com.weatherxm.data.network.ResetPasswordBody
import timber.log.Timber

interface AuthDataSource {
    suspend fun resetPassword(
        email: String
    ): Either<Failure, Unit>

    suspend fun login(
        username: String,
        password: String
    ): Either<Failure, String>

    suspend fun signup(
        username: String,
        firstName: String?,
        lastName: String?
    ): Either<Failure, String>
}

class AuthDataSourceImpl(
    private val apiService: ApiService,
    private val credentialsDatasource: CredentialsDataSource,
    private val authService: AuthService
) : AuthDataSource {

    override suspend fun login(username: String, password: String): Either<Failure, String> {
        val response = authService.login(LoginBody(username, password)).map()
        return response.map {
            Timber.d("Login success. Saving credentials.")
            credentialsDatasource.setCredentials(Credentials(username, password))
            username
        }
    }

    override suspend fun signup(
        username: String,
        firstName: String?,
        lastName: String?
    ): Either<Failure, String> {
        val response = authService.register(RegistrationBody(username, firstName, lastName)).map()
        return response.map {
            Timber.d("Signup success. Email sent to user: $username")
            username
        }
    }

    // We use apiService because authService and its interceptor are not needed
    override suspend fun resetPassword(email: String): Either<Failure, Unit> {
        return apiService.resetPassword(ResetPasswordBody(email)).map()
    }
}
