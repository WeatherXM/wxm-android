package com.weatherxm.data.network

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import com.weatherxm.data.Location
import kotlinx.parcelize.Parcelize

@JsonClass(generateAdapter = true)
@Parcelize
data class LoginBody(
    val username: String,
    val password: String
) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class RegistrationBody(
    val email: String,
    val firstName: String? = null,
    val lastName: String? = null
) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class RefreshBody(
    val refreshToken: String
) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class AccessTokenBody(
    val accessToken: String
) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class AddressBody(
    val address: String
) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class ResetPasswordBody(
    val email: String
) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class ClaimDeviceBody(
    val serialNumber: String,
    val location: Location,
    val secret: String? = null
) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class FriendlyNameBody(
    val friendlyName: String
) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class DeleteDeviceBody(
    val serialNumber: String
) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class LocationBody(
    val lat: Double,
    val lon: Double
) : Parcelable
