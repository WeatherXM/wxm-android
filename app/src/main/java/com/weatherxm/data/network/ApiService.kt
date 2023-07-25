package com.weatherxm.data.network

import co.infinum.retromock.meta.Mock
import co.infinum.retromock.meta.MockBehavior
import co.infinum.retromock.meta.MockResponse
import com.haroldadmin.cnradapter.NetworkResponse
import com.weatherxm.data.Device
import com.weatherxm.data.DeviceInfo
import com.weatherxm.data.NetworkSearchResults
import com.weatherxm.data.NetworkStatsResponse
import com.weatherxm.data.PublicDevice
import com.weatherxm.data.PublicHex
import com.weatherxm.data.TransactionsResponse
import com.weatherxm.data.User
import com.weatherxm.data.Wallet
import com.weatherxm.data.WeatherData
import com.weatherxm.data.network.interceptor.ApiRequestInterceptor.Companion.NO_AUTH_HEADER
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

@Suppress("TooManyFunctions")
interface ApiService {
    @Mock
    @MockResponse(body = "mock_files/user.json")
    @GET("/api/v1/me")
    suspend fun getUser(): NetworkResponse<User, ErrorResponse>

    @Mock
    @MockBehavior(durationDeviation = 500, durationMillis = 2000)
    @MockResponse(code = 204, body = "mock_files/empty_response.json")
    @DELETE("/api/v1/me")
    suspend fun deleteAccount(): NetworkResponse<Unit, ErrorResponse>

    @Mock
    @MockResponse(body = "mock_files/get_user_devices.json")
    @MockBehavior(durationDeviation = 500, durationMillis = 2000)
    @GET("/api/v1/me/devices")
    suspend fun getUserDevices(
        @Query("ids") deviceIds: String? = null,
    ): NetworkResponse<List<Device>, ErrorResponse>

    @Mock
    @MockBehavior(durationDeviation = 500, durationMillis = 2000)
    @MockResponse(code = 204, body = "mock_files/empty_response.json")
    @POST("/api/v1/me/devices/disclaim")
    suspend fun removeDevice(
        @Body address: DeleteDeviceBody,
    ): NetworkResponse<Unit, ErrorResponse>

    @Mock
    @MockResponse(body = "mock_files/get_user_device.json")
    @MockBehavior(durationDeviation = 500, durationMillis = 2000)
    @GET("/api/v1/me/devices/{deviceId}")
    suspend fun getUserDevice(
        @Path("deviceId") deviceId: String,
    ): NetworkResponse<Device, ErrorResponse>

    @Mock
    @MockResponse(body = "mock_files/get_user_device_info.json")
    @MockBehavior(durationDeviation = 500, durationMillis = 2000)
    @GET("/api/v1/me/devices/{deviceId}/info")
    suspend fun getUserDeviceInfo(
        @Path("deviceId") deviceId: String,
    ): NetworkResponse<DeviceInfo, ErrorResponse>

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
    @MockResponse(body = "mock_files/get_user_device_weather_forecast.json")
    @GET("/api/v1/me/devices/{deviceId}/forecast")
    suspend fun getForecast(
        @Path("deviceId") deviceId: String,
        @Query("fromDate") fromDate: String,
        @Query("toDate") toDate: String,
        @Query("exclude") exclude: String? = null,
    ): NetworkResponse<List<WeatherData>, ErrorResponse>

    @Suppress("LongParameterList")
    @Mock
    @MockResponse(body = "mock_files/get_user_device_transactions.json")
    @GET("/api/v1/me/devices/{deviceId}/tokens/transactions")
    suspend fun getTransactions(
        @Path("deviceId") deviceId: String,
        @Query("page") page: Int? = null,
        @Query("pageSize") pageSize: Int? = null,
        @Query("timezone") timezone: String? = null,
        @Query("fromDate") fromDate: String? = null,
        @Query("toDate") toDate: String? = null,
    ): NetworkResponse<TransactionsResponse, ErrorResponse>

    @Mock
    @MockResponse(body = "mock_files/get_user_device_weather_history.json")
    @GET("/api/v1/me/devices/{deviceId}/history")
    suspend fun getWeatherHistory(
        @Path("deviceId") deviceId: String,
        @Query("fromDate") fromDate: String,
        @Query("toDate") toDate: String,
        @Query("exclude") exclude: String? = "daily",
    ): NetworkResponse<List<WeatherData>, ErrorResponse>

    @Mock
    @MockResponse(body = "mock_files/claim_device.json")
    @MockBehavior(durationDeviation = 500, durationMillis = 2000)
    @POST("/api/v1/me/devices/claim")
    suspend fun claimDevice(
        @Body address: ClaimDeviceBody,
    ): NetworkResponse<Device, ErrorResponse>

    @Mock
    @MockBehavior(durationDeviation = 500, durationMillis = 2000)
    @MockResponse(code = 200, body = "mock_files/empty_response.json")
    @POST("/api/v1/me/devices/{deviceId}/friendlyName")
    suspend fun setFriendlyName(
        @Path("deviceId") deviceId: String,
        @Body friendlyName: FriendlyNameBody,
    ): NetworkResponse<Unit, ErrorResponse>

    @Mock
    @MockBehavior(durationDeviation = 500, durationMillis = 2000)
    @MockResponse(code = 200, body = "mock_files/empty_response.json")
    @DELETE("/api/v1/me/devices/{deviceId}/friendlyName")
    suspend fun clearFriendlyName(
        @Path("deviceId") deviceId: String
    ): NetworkResponse<Unit, ErrorResponse>

    @Mock
    @MockResponse(body = "mock_files/public_hexes.json")
    @MockBehavior(durationDeviation = 500, durationMillis = 2000)
    @GET("/api/v1/cells")
    @Headers(NO_AUTH_HEADER)
    suspend fun getCells(): NetworkResponse<List<PublicHex>, ErrorResponse>

    @Mock
    @MockResponse(body = "mock_files/public_devices.json")
    @MockBehavior(durationDeviation = 500, durationMillis = 2000)
    @GET("/api/v1/cells/{index}/devices")
    @Headers(NO_AUTH_HEADER)
    suspend fun getCellDevices(
        @Path("index") index: String
    ): NetworkResponse<List<PublicDevice>, ErrorResponse>

    @Mock
    @MockResponse(body = "mock_files/public_device.json")
    @MockBehavior(durationDeviation = 500, durationMillis = 2000)
    @GET("/api/v1/cells/{index}/devices/{deviceId}")
    @Headers(NO_AUTH_HEADER)
    suspend fun getCellDevice(
        @Path("index") index: String,
        @Path("deviceId") deviceId: String
    ): NetworkResponse<PublicDevice, ErrorResponse>

    @Mock
    @MockResponse(body = "mock_files/get_network_search_results.json")
    @MockBehavior(durationDeviation = 500, durationMillis = 2000)
    @GET("/api/v1/network/search")
    @Headers(NO_AUTH_HEADER)
    suspend fun networkSearch(
        @Query("query") query: String,
    ): NetworkResponse<NetworkSearchResults, ErrorResponse>

    @Suppress("LongParameterList")
    @Mock
    @MockResponse(body = "mock_files/get_user_device_transactions.json")
    @GET("/api/v1/devices/{deviceId}/tokens/transactions")
    @Headers(NO_AUTH_HEADER)
    suspend fun getPublicTransactions(
        @Path("deviceId") deviceId: String,
        @Query("page") page: Int? = null,
        @Query("pageSize") pageSize: Int? = null,
        @Query("timezone") timezone: String? = null,
        @Query("fromDate") fromDate: String? = null,
        @Query("toDate") toDate: String? = null,
    ): NetworkResponse<TransactionsResponse, ErrorResponse>

    @GET("/api/v1/me/devices/{deviceId}/firmware")
    @Streaming
    suspend fun getFirmware(
        @Path("deviceId") deviceId: String
    ): NetworkResponse<ResponseBody, ErrorResponse>

    @Mock
    @MockResponse(body = "mock_files/get_network_stats.json")
    @GET("/api/v1/network/stats")
    @Headers(NO_AUTH_HEADER)
    suspend fun getNetworkStats(
    ): NetworkResponse<NetworkStatsResponse, ErrorResponse>
}
