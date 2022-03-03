package com.weatherxm.data.network

import co.infinum.retromock.meta.Mock
import co.infinum.retromock.meta.MockBehavior
import co.infinum.retromock.meta.MockResponse
import com.haroldadmin.cnradapter.NetworkResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthService {

    @Mock
    @MockResponse(body = "mock_files/login.json")
    @POST("/api/v1/auth/login")
    suspend fun login(
        @Body credentials: LoginBody,
    ): NetworkResponse<AuthToken, ErrorResponse>

    @Mock
    @MockBehavior(durationDeviation = 500, durationMillis = 2000)
    @MockResponse(code = 200, body = "mock_files/empty_response.json")
    @POST("/api/v1/auth/register")
    suspend fun register(
        @Body registration: RegistrationBody,
    ): NetworkResponse<Unit, ErrorResponse>

    @Mock
    @MockResponse(body = "mock_files/refresh.json")
    @POST("/api/v1/auth/refresh")
    suspend fun refresh(
        @Body refresh: RefreshBody,
    ): NetworkResponse<AuthToken, ErrorResponse>
}
