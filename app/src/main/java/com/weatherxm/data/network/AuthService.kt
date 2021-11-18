package com.weatherxm.data.network

import co.infinum.retromock.meta.Mock
import co.infinum.retromock.meta.MockResponse
import com.haroldadmin.cnradapter.NetworkResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthService {

    @Mock
    @MockResponse(body = "mock_files/login.json")
    @POST("/api/auth/login")
    suspend fun login(
        @Body credentials: LoginBody,
    ): NetworkResponse<AuthToken, AuthError>

    @POST("/api/auth/register")
    suspend fun register(
        @Body registration: RegistrationBody,
    ): NetworkResponse<AuthToken, AuthError>

    @Mock
    @MockResponse(body = "mock_files/refresh.json")
    @POST("/api/auth/refresh")
    suspend fun refresh(
        @Body refresh: RefreshBody,
    ): NetworkResponse<AuthToken, AuthError>
}
