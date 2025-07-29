package com.weatherxm.data.network

import co.infinum.retromock.meta.Mock
import co.infinum.retromock.meta.MockBehavior
import co.infinum.retromock.meta.MockResponse
import com.haroldadmin.cnradapter.NetworkResponse
import com.weatherxm.data.models.BoostRewardResponse
import com.weatherxm.data.models.Device
import com.weatherxm.data.models.DeviceInfo
import com.weatherxm.data.models.DeviceRewardsSummary
import com.weatherxm.data.models.DevicesRewards
import com.weatherxm.data.models.NetworkSearchResults
import com.weatherxm.data.models.NetworkStatsResponse
import com.weatherxm.data.models.PhotoPresignedMetadata
import com.weatherxm.data.models.PublicDevice
import com.weatherxm.data.models.PublicHex
import com.weatherxm.data.models.RewardDetails
import com.weatherxm.data.models.Rewards
import com.weatherxm.data.models.RewardsTimeline
import com.weatherxm.data.models.User
import com.weatherxm.data.models.Wallet
import com.weatherxm.data.models.WalletRewards
import com.weatherxm.data.models.WeatherData
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
        @Body deleteDeviceBody: DeleteDeviceBody,
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
        @Body claimDeviceBody: ClaimDeviceBody,
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
    @MockResponse(body = "mock_files/public_cells.json")
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
        @Query("exact") exact: Boolean? = null,
        @Query("exclude") exclude: String? = null,
    ): NetworkResponse<NetworkSearchResults, ErrorResponse>

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
    ): NetworkResponse<RewardsTimeline, ErrorResponse>

    @Suppress("LongParameterList")
    @Mock
    @MockResponse(body = "mock_files/get_device_rewards.json")
    @GET("/api/v1/devices/{deviceId}/rewards")
    @Headers(NO_AUTH_HEADER)
    suspend fun getRewards(
        @Path("deviceId") deviceId: String
    ): NetworkResponse<Rewards, ErrorResponse>

    @Mock
    @MockResponse(body = "mock_files/get_device_reward_details.json")
    @GET("/api/v1/devices/{deviceId}/rewards/details")
    @Headers(NO_AUTH_HEADER)
    suspend fun getRewardDetails(
        @Path("deviceId") deviceId: String,
        @Query("date") date: String,
    ): NetworkResponse<RewardDetails, ErrorResponse>

    @Mock
    @MockResponse(body = "mock_files/get_device_reward_boost.json")
    @GET("/api/v1/devices/{deviceId}/rewards/boosts/{boostCode}")
    @Headers(NO_AUTH_HEADER)
    suspend fun getBoostReward(
        @Path("deviceId") deviceId: String,
        @Path("boostCode") boostCode: String
    ): NetworkResponse<BoostRewardResponse, ErrorResponse>

    @Mock
    @MockResponse(body = "mock_files/get_user_devices_rewards.json")
    @GET("/api/v1/me/devices/rewards")
    suspend fun getDevicesRewardsByRange(
        @Query("mode") address: String,
    ): NetworkResponse<DevicesRewards, ErrorResponse>

    @Mock
    @MockResponse(body = "mock_files/get_user_device_rewards_summary.json")
    @GET("/api/v1/me/devices/{deviceId}/rewards")
    suspend fun getDeviceRewardsByRange(
        @Path("deviceId") deviceId: String,
        @Query("mode") address: String,
    ): NetworkResponse<DeviceRewardsSummary, ErrorResponse>

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

    @Mock
    @MockResponse(body = "mock_files/wallet_rewards.json")
    @GET("/api/v1/network/rewards/withdraw")
    @Headers(NO_AUTH_HEADER)
    suspend fun getWalletRewards(
        @Query("address") address: String? = null,
    ): NetworkResponse<WalletRewards, ErrorResponse>

    @Mock
    @MockBehavior(durationDeviation = 500, durationMillis = 2000)
    @MockResponse(code = 200, body = "mock_files/empty_response.json")
    @POST("/api/v1/me/devices/{deviceId}/follow")
    suspend fun followStation(
        @Path("deviceId") deviceId: String
    ): NetworkResponse<Unit, ErrorResponse>

    @Mock
    @MockBehavior(durationDeviation = 500, durationMillis = 2000)
    @MockResponse(code = 200, body = "mock_files/get_user_device.json")
    @POST("/api/v1/me/devices/{deviceId}/location")
    suspend fun setLocation(
        @Path("deviceId") deviceId: String,
        @Body location: LocationBody
    ): NetworkResponse<Device, ErrorResponse>

    @Mock
    @MockBehavior(durationDeviation = 500, durationMillis = 2000)
    @MockResponse(code = 200, body = "mock_files/empty_response.json")
    @DELETE("/api/v1/me/devices/{deviceId}/follow")
    suspend fun unfollowStation(
        @Path("deviceId") deviceId: String
    ): NetworkResponse<Unit, ErrorResponse>

    @Mock
    @MockBehavior(durationDeviation = 500, durationMillis = 2000)
    @MockResponse(code = 200, body = "mock_files/empty_response.json")
    @POST("/api/v1/me/notifications/fcm/installations/{installationId}/tokens/{fcmToken}")
    suspend fun setFcmToken(
        @Path("installationId") installationId: String,
        @Path("fcmToken") fcmToken: String
    ): NetworkResponse<Unit, ErrorResponse>

    @Mock
    @MockBehavior(durationDeviation = 500, durationMillis = 2000)
    @MockResponse(code = 200, body = "mock_files/empty_response.json")
    @POST("/api/v1/me/devices/frequency")
    suspend fun setDeviceFrequency(
        @Body deviceFrequency: DeviceFrequencyBody
    ): NetworkResponse<Unit, ErrorResponse>

    @Mock
    @MockBehavior(durationDeviation = 500, durationMillis = 2000)
    @MockResponse(code = 200, body = "mock_files/device_photos_list.json")
    @GET("/api/v1/me/devices/{deviceId}/photos")
    suspend fun getDevicePhotos(
        @Path("deviceId") deviceId: String
    ): NetworkResponse<List<String>, ErrorResponse>

    @Mock
    @MockBehavior(durationDeviation = 500, durationMillis = 2000)
    @MockResponse(code = 204, body = "mock_files/empty_response.json")
    @DELETE("/api/v1/me/devices/{deviceId}/photos/{photoId}")
    suspend fun deleteDevicePhoto(
        @Path("deviceId") deviceId: String,
        @Path("photoId") photoName: String
    ): NetworkResponse<Unit, ErrorResponse>

    @Mock
    @MockBehavior(durationDeviation = 500, durationMillis = 2000)
    @MockResponse(code = 204, body = "mock_files/get_photos_presigned_metadata.json")
    @POST("/api/v1/me/devices/{deviceId}/photos")
    suspend fun getPhotosMetadataForUpload(
        @Path("deviceId") deviceId: String,
        @Body names: PhotoNamesBody
    ): NetworkResponse<List<PhotoPresignedMetadata>, ErrorResponse>

    @Mock
    @MockResponse(body = "mock_files/get_location_weather_forecast.json")
    @MockBehavior(durationDeviation = 500, durationMillis = 2000)
    @GET("/api/v1/cells/forecast")
    @Headers(NO_AUTH_HEADER)
    suspend fun getLocationForecast(
        @Path("lat") lat: Double,
        @Path("lon") lon: Double,
    ): NetworkResponse<List<WeatherData>, ErrorResponse>
}
