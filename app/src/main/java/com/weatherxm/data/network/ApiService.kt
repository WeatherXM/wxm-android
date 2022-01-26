package com.weatherxm.data.network

import co.infinum.retromock.meta.Mock
import co.infinum.retromock.meta.MockResponse
import com.haroldadmin.cnradapter.NetworkResponse
import com.weatherxm.data.Device
import com.weatherxm.data.Tokens
import com.weatherxm.data.User
import com.weatherxm.data.WeatherData
import com.weatherxm.data.network.interceptor.ApiRequestInterceptor.Companion.NO_AUTH_HEADER
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

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

    @POST("/api/auth/resetPassword")
    suspend fun resetPassword(
        @Body refresh: ResetPasswordBody,
    ): NetworkResponse<Unit, ErrorResponse>

    @Mock
    @MockResponse(body = "mock_files/get_user_device_weather_current_forecast_short_term.json")
    @GET("/api/me/devices/{deviceId}/forecast")
    suspend fun getForecast(
        @Path("deviceId") deviceId: String,
        @Query("fromDate") fromDate: String,
        @Query("toDate") toDate: String,
        @Query("exclude") exclude: String? = null,
    ): NetworkResponse<List<WeatherData>, ErrorResponse>

    @Mock
    @MockResponse(body = "mock_files/get_user_device_tokens.json")
    @GET("/api/me/devices/{deviceId}/tokens/summary")
    suspend fun getTokensSummary(
        @Path("deviceId") deviceId: String,
    ): NetworkResponse<Tokens, ErrorResponse>

    @Mock
    @MockResponse(body = "mock_files/get_user_device_weather_history.json")
    @GET("/api/me/devices/{deviceId}/history")
    suspend fun getWeatherHistory(
        @Path("deviceId") deviceId: String,
        @Query("fromDate") fromDate: String,
        @Query("toDate") toDate: String,
        @Query("exclude") exclude: String? = null,
    ): NetworkResponse<List<WeatherData>, ErrorResponse>
}
