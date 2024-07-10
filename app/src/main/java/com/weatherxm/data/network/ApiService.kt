package com.weatherxm.data.network

import arrow.core.Either
import co.infinum.retromock.meta.Mock
import co.infinum.retromock.meta.MockBehavior
import co.infinum.retromock.meta.MockResponse
import com.weatherxm.data.BoostRewardResponse
import com.weatherxm.data.Device
import com.weatherxm.data.DeviceInfo
import com.weatherxm.data.NetworkSearchResults
import com.weatherxm.data.NetworkStatsResponse
import com.weatherxm.data.PublicDevice
import com.weatherxm.data.PublicHex
import com.weatherxm.data.RewardDetails
import com.weatherxm.data.Rewards
import com.weatherxm.data.RewardsTimeline
import com.weatherxm.data.User
import com.weatherxm.data.Wallet
import com.weatherxm.data.WalletRewards
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
    suspend fun getUser(): Either<Throwable, User>

    @Mock
    @MockBehavior(durationDeviation = 500, durationMillis = 2000)
    @MockResponse(code = 204, body = "mock_files/empty_response.json")
    @DELETE("/api/v1/me")
    suspend fun deleteAccount(): Either<Throwable, Unit>

    @Mock
    @MockResponse(body = "mock_files/get_user_devices.json")
    @MockBehavior(durationDeviation = 500, durationMillis = 2000)
    @GET("/api/v1/me/devices")
    suspend fun getUserDevices(
        @Query("ids") deviceIds: String? = null,
    ): Either<Throwable, List<Device>>

    @Mock
    @MockBehavior(durationDeviation = 500, durationMillis = 2000)
    @MockResponse(code = 204, body = "mock_files/empty_response.json")
    @POST("/api/v1/me/devices/disclaim")
    suspend fun removeDevice(
        @Body address: DeleteDeviceBody,
    ): Either<Throwable, Unit>

    @Mock
    @MockResponse(body = "mock_files/get_user_device.json")
    @MockBehavior(durationDeviation = 500, durationMillis = 2000)
    @GET("/api/v1/me/devices/{deviceId}")
    suspend fun getUserDevice(
        @Path("deviceId") deviceId: String,
    ): Either<Throwable, Device>

    @Mock
    @MockResponse(body = "mock_files/get_user_device_info.json")
    @MockBehavior(durationDeviation = 500, durationMillis = 2000)
    @GET("/api/v1/me/devices/{deviceId}/info")
    suspend fun getUserDeviceInfo(
        @Path("deviceId") deviceId: String,
    ): Either<Throwable, DeviceInfo>

    @Mock
    @MockBehavior(durationDeviation = 300, durationMillis = 1000)
    @MockResponse(code = 200, body = "mock_files/get_wallet.json")
    @GET("/api/v1/me/wallet")
    suspend fun getWallet(): Either<Throwable, Wallet>

    @Mock
    @MockBehavior(durationDeviation = 500, durationMillis = 2000)
    @MockResponse(code = 200, body = "mock_files/empty_response.json")
    @POST("/api/v1/me/wallet")
    suspend fun setWallet(
        @Body address: AddressBody,
    ): Either<Throwable, Unit>

    @Mock
    @MockResponse(body = "mock_files/get_user_device_weather_forecast.json")
    @GET("/api/v1/me/devices/{deviceId}/forecast")
    suspend fun getForecast(
        @Path("deviceId") deviceId: String,
        @Query("fromDate") fromDate: String,
        @Query("toDate") toDate: String,
        @Query("exclude") exclude: String? = null,
    ): Either<Throwable, List<WeatherData>>

    @Mock
    @MockResponse(body = "mock_files/get_user_device_weather_history.json")
    @GET("/api/v1/me/devices/{deviceId}/history")
    suspend fun getWeatherHistory(
        @Path("deviceId") deviceId: String,
        @Query("fromDate") fromDate: String,
        @Query("toDate") toDate: String,
        @Query("exclude") exclude: String? = "daily",
    ): Either<Throwable, List<WeatherData>>

    @Mock
    @MockResponse(body = "mock_files/claim_device.json")
    @MockBehavior(durationDeviation = 500, durationMillis = 2000)
    @POST("/api/v1/me/devices/claim")
    suspend fun claimDevice(
        @Body address: ClaimDeviceBody,
    ): Either<Throwable, Device>

    @Mock
    @MockBehavior(durationDeviation = 500, durationMillis = 2000)
    @MockResponse(code = 200, body = "mock_files/empty_response.json")
    @POST("/api/v1/me/devices/{deviceId}/friendlyName")
    suspend fun setFriendlyName(
        @Path("deviceId") deviceId: String,
        @Body friendlyName: FriendlyNameBody,
    ): Either<Throwable, Unit>

    @Mock
    @MockBehavior(durationDeviation = 500, durationMillis = 2000)
    @MockResponse(code = 200, body = "mock_files/empty_response.json")
    @DELETE("/api/v1/me/devices/{deviceId}/friendlyName")
    suspend fun clearFriendlyName(
        @Path("deviceId") deviceId: String
    ): Either<Throwable, Unit>

    @Mock
    @MockResponse(body = "mock_files/public_cells.json")
    @MockBehavior(durationDeviation = 500, durationMillis = 2000)
    @GET("/api/v1/cells")
    @Headers(NO_AUTH_HEADER)
    suspend fun getCells(): Either<Throwable, List<PublicHex>>

    @Mock
    @MockResponse(body = "mock_files/public_devices.json")
    @MockBehavior(durationDeviation = 500, durationMillis = 2000)
    @GET("/api/v1/cells/{index}/devices")
    @Headers(NO_AUTH_HEADER)
    suspend fun getCellDevices(
        @Path("index") index: String
    ): Either<Throwable, List<PublicDevice>>

    @Mock
    @MockResponse(body = "mock_files/public_device.json")
    @MockBehavior(durationDeviation = 500, durationMillis = 2000)
    @GET("/api/v1/cells/{index}/devices/{deviceId}")
    @Headers(NO_AUTH_HEADER)
    suspend fun getCellDevice(
        @Path("index") index: String,
        @Path("deviceId") deviceId: String
    ): Either<Throwable, PublicDevice>

    @Mock
    @MockResponse(body = "mock_files/get_network_search_results.json")
    @MockBehavior(durationDeviation = 500, durationMillis = 2000)
    @GET("/api/v1/network/search")
    @Headers(NO_AUTH_HEADER)
    suspend fun networkSearch(
        @Query("query") query: String,
        @Query("exact") exact: Boolean? = null,
        @Query("exclude") exclude: String? = null,
    ): Either<Throwable, NetworkSearchResults>

    @Suppress("LongParameterList")
    @Mock
    @MockResponse(body = "mock_files/get_device_rewards_timeline.json")
    @GET("/api/v1/devices/{deviceId}/rewards/timeline")
    @Headers(NO_AUTH_HEADER)
    suspend fun getRewardsTimeline(
        @Path("deviceId") deviceId: String,
        @Query("page") page: Int? = null,
        @Query("pageSize") pageSize: Int? = null,
        @Query("timezone") timezone: String? = null,
        @Query("fromDate") fromDate: String? = null,
        @Query("toDate") toDate: String? = null,
    ): Either<Throwable, RewardsTimeline>

    @Suppress("LongParameterList")
    @Mock
    @MockResponse(body = "mock_files/get_device_rewards.json")
    @GET("/api/v1/devices/{deviceId}/rewards")
    @Headers(NO_AUTH_HEADER)
    suspend fun getRewards(
        @Path("deviceId") deviceId: String
    ): Either<Throwable, Rewards>

    @Mock
    @MockResponse(body = "mock_files/get_device_reward_details.json")
    @GET("/api/v1/devices/{deviceId}/rewards/details")
    @Headers(NO_AUTH_HEADER)
    suspend fun getRewardDetails(
        @Path("deviceId") deviceId: String,
        @Query("date") date: String,
    ): Either<Throwable, RewardDetails>

    @Mock
    @MockResponse(body = "mock_files/get_device_reward_boost.json")
    @GET("/api/v1/devices/{deviceId}/rewards/boosts/{boostCode}")
    @Headers(NO_AUTH_HEADER)
    suspend fun getRewardBoost(
        @Path("deviceId") deviceId: String,
        @Path("boostCode") boostCode: String
    ): Either<Throwable, BoostRewardResponse>

    @GET("/api/v1/me/devices/{deviceId}/firmware")
    @Streaming
    suspend fun getFirmware(
        @Path("deviceId") deviceId: String
    ): Either<Throwable, ResponseBody>

    @Mock
    @MockResponse(body = "mock_files/get_network_stats.json")
    @GET("/api/v1/network/stats")
    @Headers(NO_AUTH_HEADER)
    suspend fun getNetworkStats(
    ): Either<Throwable, NetworkStatsResponse>

    @Mock
    @MockResponse(body = "mock_files/wallet_rewards.json")
    @GET("/api/v1/network/rewards/withdraw")
    @Headers(NO_AUTH_HEADER)
    suspend fun getWalletRewards(
        @Query("address") address: String? = null,
    ): Either<Throwable, WalletRewards>

    @Mock
    @MockBehavior(durationDeviation = 500, durationMillis = 2000)
    @MockResponse(code = 200, body = "mock_files/empty_response.json")
    @POST("/api/v1/me/devices/{deviceId}/follow")
    suspend fun followStation(
        @Path("deviceId") deviceId: String
    ): Either<Throwable, Unit>

    @Mock
    @MockBehavior(durationDeviation = 500, durationMillis = 2000)
    @MockResponse(code = 200, body = "mock_files/get_user_device.json")
    @POST("/api/v1/me/devices/{deviceId}/location")
    suspend fun setLocation(
        @Path("deviceId") deviceId: String,
        @Body location: LocationBody
    ): Either<Throwable, Device>

    @Mock
    @MockBehavior(durationDeviation = 500, durationMillis = 2000)
    @MockResponse(code = 200, body = "mock_files/empty_response.json")
    @DELETE("/api/v1/me/devices/{deviceId}/follow")
    suspend fun unfollowStation(
        @Path("deviceId") deviceId: String
    ): Either<Throwable, Unit>

    @Mock
    @MockBehavior(durationDeviation = 500, durationMillis = 2000)
    @MockResponse(code = 200, body = "mock_files/empty_response.json")
    @POST("/api/v1/me/notifications/fcm/installations/{installationId}/tokens/{fcmToken}")
    suspend fun setFcmToken(
        @Path("installationId") installationId: String,
        @Path("fcmToken") fcmToken: String
    ): Either<Throwable, Unit>

    @Mock
    @MockBehavior(durationDeviation = 500, durationMillis = 2000)
    @MockResponse(code = 200, body = "mock_files/empty_response.json")
    @DELETE("/api/v1/me/notifications/fcm/installations/{installationId}/tokens/{fcmToken}")
    suspend fun deleteFcmToken(
        @Path("installationId") installationId: String,
        @Path("fcmToken") fcmToken: String
    ): Either<Throwable, Unit>
}
