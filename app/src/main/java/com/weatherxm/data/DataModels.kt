@file:Suppress("MatchingDeclarationName")

package com.weatherxm.data

import android.os.Parcelable
import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import timber.log.Timber

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

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class WXMRemoteMessage(
    val type: RemoteMessageType,
    val url: String? = null
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

enum class RemoteMessageType(val id: String, val publicName: String, val desc: String) {
    ANNOUNCEMENT(
        "announcement",
        "Announcements",
        "These notifications are used for WeatherXM-related announcements."
    ),
    DEFAULT("DEFAULT", "Default", "These are general purpose notifications.");

    companion object {
        fun parse(id: String): RemoteMessageType {
            return try {
                RemoteMessageType.entries.firstOrNull {
                    it.id.equals(id, true)
                } ?: DEFAULT
            } catch (e: IllegalArgumentException) {
                Timber.e(e)
                DEFAULT
            }
        }
    }
}
