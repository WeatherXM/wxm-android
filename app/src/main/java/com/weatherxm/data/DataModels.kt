@file:Suppress("MatchingDeclarationName")

package com.weatherxm.data

import android.os.Parcelable
import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class CountryAndFrequencies(
    val country: String?,
    val recommendedFrequency: Frequency,
    val otherFrequencies: List<Frequency>,
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
data class OTAState(
    val state: BluetoothOTAState,
    val progress: Int,
    var error: Int? = null,
    var errorType: Int? = null,
    var message: String? = null
)

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class CountryInfo(
    val code: String,
    @Json(name = "helium_frequency")
    val heliumFrequency: String?,
    @Json(name = "map_center")
    var mapCenter: Location?,
) : Parcelable

enum class Frequency {
    EU868,
    US915,
    AU915,
    CN470,
    KR920,
    IN865,
    RU864,
    AS923_1,
    AS923_2,
    AS923_3,
    AS923_4
}

enum class BluetoothOTAState {
    IN_PROGRESS,
    FAILED,
    ABORTED,
    COMPLETED
}
