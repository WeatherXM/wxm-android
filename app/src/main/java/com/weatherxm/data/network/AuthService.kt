package com.weatherxm.data.network

import com.haroldadmin.cnradapter.NetworkResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthService {

    @POST("/api/auth/login")
    suspend fun login(
        @Body credentials: LoginBody,
    ): NetworkResponse<AuthToken, AuthError>

    @POST("/api/auth/register")
    suspend fun register(
        @Body registration: RegistrationBody,
    ): NetworkResponse<AuthToken, AuthError>

    @POST("/api/auth/refresh")
    suspend fun refresh(
        @Body refresh: RefreshBody,
    ): NetworkResponse<AuthToken, AuthError>
}
