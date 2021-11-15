package com.weatherxm.data.network

import com.haroldadmin.cnradapter.NetworkResponse
import com.weatherxm.data.User
import com.weatherxm.data.Device
import com.weatherxm.data.network.interceptor.ApiRequestInterceptor.Companion.NO_AUTH_HEADER
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.Path

interface ApiService {
    @GET("/api/me")
    suspend fun getUser(): NetworkResponse<User, ErrorResponse>

    @GET("/api/me/devices")
    suspend fun getUserDevices(): NetworkResponse<List<Device>, ErrorResponse>

    @GET("/api/me/devices/{deviceId}")
    suspend fun getUserDevice(
        @Path("deviceId") deviceId: String,
    ): NetworkResponse<Device, ErrorResponse>

    @GET("/api/devices/")
    @Headers(NO_AUTH_HEADER)
    suspend fun getPublicDevices(): NetworkResponse<List<Device>, ErrorResponse>

    @POST("/api/me/wallet")
    suspend fun saveAddress(
        @Body address: AddressBody,
    ): NetworkResponse<Unit, ErrorResponse>
}
