package com.weatherxm.data.network

import com.auth0.android.jwt.DecodeException
import com.auth0.android.jwt.JWT
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import timber.log.Timber

@JsonClass(generateAdapter = true)
data class Credentials(
    @Json(name = "username") val username: String,
    @Json(name = "password") val password: String
) {
    fun isValid(): Boolean = username.isNotEmpty() && password.isNotEmpty()
}

@JsonClass(generateAdapter = true)
data class AuthToken(
    @Json(name = "token") val access: String,
    @Json(name = "refreshToken") val refresh: String
) {
    fun isAccessTokenValid(): Boolean = isTokenValid(access)

    fun isRefreshTokenValid(): Boolean = isTokenValid(refresh)

    private fun isTokenValid(token: String): Boolean {
        return try {
            !JWT(token).isExpired(0)
        } catch (e: DecodeException) {
            Timber.w(e, "Invalid auth token.")
            false
        }
    }
}

@JsonClass(generateAdapter = true)
data class AuthError(
    @Json(name = "status") val status: Int,
    @Json(name = "message") val message: String,
    @Json(name = "error") val errorCode: String,
    @Json(name = "timestamp") val timestamp: String
)

@JsonClass(generateAdapter = true)
data class ErrorResponse(
    @Json(name = "error") val error: String,
    @Json(name = "message") val message: String
)

@JsonClass(generateAdapter = true)
data class PagedResponse<T>(
    val data: T,
    val totalPages: Int,
    val totalElements: Int,
    val hasNext: Boolean
)
