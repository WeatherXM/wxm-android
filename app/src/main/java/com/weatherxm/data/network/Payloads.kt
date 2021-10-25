package com.weatherxm.data.network

import android.os.Parcelable
import com.squareup.moshi.JsonClass
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
    val username: String,
    val password: String,
    val firstName: String? = "",
    val lastName: String? = ""
) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class RefreshBody(
    val refreshToken: String
) : Parcelable
