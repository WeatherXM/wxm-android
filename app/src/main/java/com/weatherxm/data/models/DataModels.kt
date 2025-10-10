@file:Suppress("MatchingDeclarationName")

package com.weatherxm.data.models

import android.os.Parcelable
import androidx.annotation.Keep
import com.google.firebase.firestore.PropertyName
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.data.otherFrequencies
import kotlinx.parcelize.Parcelize
import timber.log.Timber

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class CountryAndFrequencies(
    val country: String?,
    val recommendedFrequency: Frequency,
    val otherFrequencies: List<Frequency>,
) : Parcelable {
    companion object {
        fun default(): CountryAndFrequencies {
            return CountryAndFrequencies(null, Frequency.US915, otherFrequencies(Frequency.US915))
        }
    }
}

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
    val url: String? = null,
    val deviceId: String? = null
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class Survey(
    val id: String,
    val title: String,
    val message: String,
    val actionLabel: String,
    val url: String
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class RemoteBanner(
    val id: String,
    val title: String,
    val message: String,
    val actionLabel: String,
    val url: String,
    val showActionButton: Boolean,
    val showCloseButton: Boolean
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
data class QuestWithStepsFirestore(
    val questData: QuestFirestore,
    val steps: List<QuestFirestoreStep>
)

@Keep
@JsonClass(generateAdapter = true)
data class QuestFirestore(
    val title: String? = null,
    val description: String? = null,
    @get:PropertyName("isActive") // https://medium.com/@eeddeellee/boolean-fields-that-start-with-is-in-firebase-firestore-49afb65e3639
    val isActive: Boolean? = null
)

@Keep
@JsonClass(generateAdapter = true)
data class QuestFirestoreStep(
    var stepId: String? = null,
    val title: String? = null,
    val description: String? = null,
    val tokens: Int? = null,
    @get:PropertyName("isOptional") // https://medium.com/@eeddeellee/boolean-fields-that-start-with-is-in-firebase-firestore-49afb65e3639
    val isOptional: Boolean? = null,
    val type: String? = null
)

@Keep
@JsonClass(generateAdapter = true)
data class QuestUser(
    val uid: String? = null,
    val earnedTokens: Int? = null,
    val paidTokens: Int? = null
)

@Keep
@JsonClass(generateAdapter = true)
data class QuestUserWallet(
    val address: String? = null
)

@Keep
@JsonClass(generateAdapter = true)
data class QuestUserProgress(
    @get:PropertyName("isCompleted") // https://medium.com/@eeddeellee/boolean-fields-that-start-with-is-in-firebase-firestore-49afb65e3639
    val isCompleted: Boolean? = null,
    val completedSteps: List<String>? = null,
    val skippedSteps: List<String>? = null
)

enum class RemoteBannerType {
    INFO_BANNER,
    ANNOUNCEMENT
}

enum class Frequency {
    EU868,
    US915,
    AU915,
    CN470,
    KR920,
    IN865,
    RU864,
    AS923_1,
    AS923_1B,
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

enum class DeviceNotificationType(val analyticsParam: AnalyticsService.ParamValue) {
    ACTIVITY(AnalyticsService.ParamValue.ACTIVITY),
    BATTERY(AnalyticsService.ParamValue.LOW_BATTERY_ID),
    FIRMWARE(AnalyticsService.ParamValue.OTA_UPDATE_ID),
    HEALTH(AnalyticsService.ParamValue.STATION_HEALTH)
}

enum class RemoteMessageType(val id: String, val publicName: String, val desc: String) {
    ANNOUNCEMENT(
        "announcement",
        "Announcements",
        "These notifications are used for WeatherXM-related announcements."
    ),
    STATION(
        "station",
        "Station Notifications",
        "These notifications are used for announcements or alerts regarding your station(s)."
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
