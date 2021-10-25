package com.weatherxm.data.network

import com.haroldadmin.cnradapter.NetworkResponse
import com.weatherxm.data.User
import com.weatherxm.data.Device
import com.weatherxm.data.PublicDevice
import com.weatherxm.data.network.interceptor.ApiRequestInterceptor.Companion.NO_AUTH_HEADER
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @GET("/api/auth/user")
    suspend fun user(): NetworkResponse<User, ErrorResponse>

    @GET("/api/customer/{id}/devices/")
    suspend fun getCustomerDevices(
        @Path("id") customerId: String,
        @Query("type") type: String = "Weather Station",
        @Query("textSearch") search: String? = null,
        @Query("pageSize") pageSize: Int = 500,
        @Query("page") page: Int = 0
    ): NetworkResponse<PagedResponse<List<Device>>, ErrorResponse>

    @GET("/api/devices/")
    @Headers(NO_AUTH_HEADER)
    suspend fun getPublicDevices(): NetworkResponse<List<PublicDevice>, ErrorResponse>
}
