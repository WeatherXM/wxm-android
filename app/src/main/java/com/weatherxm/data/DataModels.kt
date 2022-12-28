@file:Suppress("MatchingDeclarationName")
package com.weatherxm.data

import android.os.Parcelable
import androidx.annotation.Keep
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@Suppress("MagicNumber")
@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class LastAndDatedTxs(
    val lastTx: Transaction?,
    val datedTxs: List<Pair<String, Float>>,
) : Parcelable

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
