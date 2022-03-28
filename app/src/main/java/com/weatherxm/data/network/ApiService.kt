package com.weatherxm.data.network

import co.infinum.retromock.meta.Mock
import co.infinum.retromock.meta.MockBehavior
import co.infinum.retromock.meta.MockResponse
import com.haroldadmin.cnradapter.NetworkResponse
import com.weatherxm.data.Device
import com.weatherxm.data.Tokens
import com.weatherxm.data.User
import com.weatherxm.data.Wallet
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
    @GET("/api/v1/me")
    suspend fun getUser(): NetworkResponse<User, ErrorResponse>

    @Mock
    @MockResponse(body = "mock_files/get_user_devices.json")
    @MockBehavior(durationDeviation = 500, durationMillis = 2000)
    @GET("/api/v1/me/devices")
    suspend fun getUserDevices(): NetworkResponse<List<Device>, ErrorResponse>

    @GET("/api/v1/me/devices/{deviceId}")
    suspend fun getUserDevice(
        @Path("deviceId") deviceId: String,
    ): NetworkResponse<Device, ErrorResponse>

    @Mock
    @MockResponse(body = "mock_files/public_devices.json")
    @GET("/api/v1/devices/")
    @Headers(NO_AUTH_HEADER)
    suspend fun getPublicDevices(): NetworkResponse<List<Device>, ErrorResponse>

    @Mock
    @MockBehavior(durationDeviation = 300, durationMillis = 1000)
    @MockResponse(code = 200, body = "mock_files/get_wallet.json")
    @GET("/api/v1/me/wallet")
    suspend fun getWallet(): NetworkResponse<Wallet, ErrorResponse>

    @Mock
    @MockBehavior(durationDeviation = 500, durationMillis = 2000)
    @MockResponse(code = 200, body = "mock_files/empty_response.json")
    @POST("/api/v1/me/wallet")
    suspend fun setWallet(
        @Body address: AddressBody,
    ): NetworkResponse<Unit, ErrorResponse>

    @Mock
    @MockBehavior(durationDeviation = 500, durationMillis = 2000)
    @MockResponse(code = 200, body = "mock_files/empty_response.json")
    @POST("/api/v1/auth/resetPassword")
    suspend fun resetPassword(
        @Body resetPasswordBody: ResetPasswordBody,
    ): NetworkResponse<Unit, ErrorResponse>

    @Mock
    @MockResponse(body = "mock_files/get_user_device_weather_forecast.json")
    @GET("/api/v1/me/devices/{deviceId}/forecast")
    suspend fun getForecast(
        @Path("deviceId") deviceId: String,
        @Query("fromDate") fromDate: String,
        @Query("toDate") toDate: String,
        @Query("exclude") exclude: String? = null,
    ): NetworkResponse<List<WeatherData>, ErrorResponse>

    @Mock
    @MockResponse(body = "mock_files/get_user_device_tokens.json")
    @GET("/api/v1/me/devices/{deviceId}/tokens/summary")
    suspend fun getTokensSummary(
        @Path("deviceId") deviceId: String,
    ): NetworkResponse<Tokens, ErrorResponse>

    @Mock
    @MockResponse(body = "mock_files/get_user_device_weather_history.json")
    @GET("/api/v1/me/devices/{deviceId}/history")
    suspend fun getWeatherHistory(
        @Path("deviceId") deviceId: String,
        @Query("fromDate") fromDate: String,
        @Query("toDate") toDate: String,
        @Query("exclude") exclude: String? = null,
    ): NetworkResponse<List<WeatherData>, ErrorResponse>

    @Mock
    @MockResponse(body = "mock_files/claim_device.json")
    @MockBehavior(durationDeviation = 500, durationMillis = 2000)
    @POST("/api/v1/me/devices/claim")
    suspend fun claimDevice(
        @Body address: ClaimDeviceBody,
    ): NetworkResponse<Device, ErrorResponse>
}
