package com.weatherxm.data.network

import com.auth0.android.jwt.DecodeException
import com.auth0.android.jwt.JWT
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.weatherxm.data.details
import timber.log.Timber

@JsonClass(generateAdapter = true)
data class AuthToken(
    @Json(name = "token") val access: String,
    @Json(name = "refreshToken") val refresh: String
) {
    fun isAccessTokenValid(): Boolean = isTokenValid(access)

    fun isRefreshTokenValid(): Boolean = isTokenValid(refresh)

    private fun isTokenValid(token: String): Boolean {
        return try {
            val jwt = JWT(token)
            val isExpired = jwt.isExpired(0)
            Timber.d("Token ${if (isExpired) "expired" else "valid"} [${jwt.details()}]")
            !isExpired
        } catch (e: DecodeException) {
            Timber.w(e, "Invalid auth token.")
            false
        }
    }
}

@JsonClass(generateAdapter = true)
data class ErrorResponse(
    @Json(name = "code") val code: String,
    @Json(name = "message") val message: String,
    @Json(name = "id") val id: String,
    @Json(name = "path") val path: String
) {
    companion object {
        const val INVALID_USERNAME = "InvalidUsername"
        const val INVALID_PASSWORD = "InvalidPassword"
        const val INVALID_CREDENTIALS = "InvalidCredentials"
        const val USER_ALREADY_EXISTS = "UserAlreadyExists"
        const val INVALID_ACCESS_TOKEN = "InvalidAccessToken"
        const val INVALID_ACTIVATION_TOKEN = "InvalidActivationToken"
        const val DEVICE_NOT_FOUND = "DeviceNotFound"
        const val INVALID_WALLET_ADDRESS = "InvalidWalletAddress"
        const val INVALID_FRIENDLY_NAME = "InvalidFriendlyName"
        const val INVALID_FROM_DATE = "InvalidFromDate"
        const val INVALID_TO_DATE = "InvalidToDate"
        const val INVALID_TIMEZONE = "InvalidTimezone"
        const val INVALID_CLAIM_ID = "InvalidClaimId"
        const val INVALID_CLAIM_LOCATION = "InvalidClaimLocation"
        const val DEVICE_ALREADY_CLAIMED = "DeviceAlreadyClaimed"
        const val DEVICE_CLAIMING = "DeviceClaiming"
        const val UNAUTHORIZED = "Unauthorized"
        const val USER_NOT_FOUND = "UserNotFound"
        const val FORBIDDEN = "Forbidden"
        const val VALIDATION = "Validation"
        const val NOT_FOUND = "NotFound"
        const val MAX_FOLLOWED = "MaxFollowed"
        const val WALLET_ADDRESS_NOT_FOUND = "WalletAddressNotFound"
    }

    override fun toString(): String {
        return "[$path] [$id] $code: $message"
    }
}
