package com.weatherxm.data.network

import co.infinum.retromock.meta.Mock
import co.infinum.retromock.meta.MockResponse
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
    @Mock
    @MockResponse(body = "mock_files/user.json")
    @GET("/api/me")
    suspend fun getUser(): NetworkResponse<User, ErrorResponse>

    @Mock
    @MockResponse(body = "mock_files/get_user_devices.json")
    @GET("/api/me/devices")
    suspend fun getUserDevices(): NetworkResponse<List<Device>, ErrorResponse>

    @GET("/api/me/devices/{deviceId}")
    suspend fun getUserDevice(
        @Path("deviceId") deviceId: String,
    ): NetworkResponse<Device, ErrorResponse>

    @Mock
    @MockResponse(body = "mock_files/public_devices.json")
    @GET("/api/devices/")
    @Headers(NO_AUTH_HEADER)
    suspend fun getPublicDevices(): NetworkResponse<List<Device>, ErrorResponse>

    @POST("/api/me/wallet")
    suspend fun saveAddress(
        @Body address: AddressBody,
    ): NetworkResponse<Unit, ErrorResponse>
}
