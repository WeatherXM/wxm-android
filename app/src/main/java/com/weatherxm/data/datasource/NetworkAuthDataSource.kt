package com.weatherxm.data.datasource

import arrow.core.Either
import com.weatherxm.data.models.Failure
import com.weatherxm.data.mapResponse
import com.weatherxm.data.network.AuthService
import com.weatherxm.data.network.AuthToken
import com.weatherxm.data.network.LoginBody
import com.weatherxm.data.network.LogoutBody
import com.weatherxm.data.network.RefreshBody
import com.weatherxm.data.network.RegistrationBody
import com.weatherxm.data.network.ResetPasswordBody

class NetworkAuthDataSource(private val authService: AuthService) : AuthDataSource {

    override suspend fun refresh(authToken: AuthToken): Either<Failure, AuthToken> {
        return authService.refresh(RefreshBody(authToken.refresh)).mapResponse()
    }

    override suspend fun login(username: String, password: String): Either<Failure, AuthToken> {
        return authService.login(LoginBody(username, password)).mapResponse()
    }

    override suspend fun signup(
        username: String,
        firstName: String?,
        lastName: String?
    ): Either<Failure, Unit> {
        return authService.register(RegistrationBody(username, firstName, lastName)).mapResponse()
    }

    override suspend fun logout(
        accessToken: String,
        installationId: String?
    ): Either<Failure, Unit> {
        return authService.logout(LogoutBody(accessToken, installationId)).mapResponse()
    }

    override suspend fun resetPassword(email: String): Either<Failure, Unit> {
        return authService.resetPassword(ResetPasswordBody(email)).mapResponse()
    }

    override fun getAuthToken(): Either<Failure, AuthToken> {
        throw NotImplementedError("Won't be implemented. Ignore this.")
    }

    override fun setAuthToken(token: AuthToken) {
        throw NotImplementedError("Won't be implemented. Ignore this.")
    }
}
