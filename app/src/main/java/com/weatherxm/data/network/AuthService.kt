package com.weatherxm.data.network

import arrow.core.Either
import co.infinum.retromock.meta.Mock
import co.infinum.retromock.meta.MockBehavior
import co.infinum.retromock.meta.MockResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthService {

    @Mock
    @MockResponse(body = "mock_files/login.json")
    @POST("/api/v1/auth/login")
    suspend fun login(
        @Body credentials: LoginBody,
    ): Either<Throwable, AuthToken>

    @Mock
    @MockBehavior(durationDeviation = 500, durationMillis = 2000)
    @MockResponse(code = 200, body = "mock_files/empty_response.json")
    @POST("/api/v1/auth/register")
    suspend fun register(
        @Body registration: RegistrationBody,
    ): Either<Throwable, Unit>

    @Mock
    @MockResponse(body = "mock_files/refresh.json")
    @POST("/api/v1/auth/refresh")
    suspend fun refresh(
        @Body refresh: RefreshBody,
    ): Either<Throwable, AuthToken>

    @Mock
    @MockResponse(body = "mock_files/empty_response.json")
    @POST("/api/v1/auth/logout")
    suspend fun logout(
        @Body accessToken: AccessTokenBody,
    ): Either<Throwable, Unit>

    @Mock
    @MockResponse(code = 200, body = "mock_files/empty_response.json")
    @POST("/api/v1/auth/resetPassword")
    suspend fun resetPassword(
        @Body resetPasswordBody: ResetPasswordBody,
    ): Either<Throwable, Unit>
}
