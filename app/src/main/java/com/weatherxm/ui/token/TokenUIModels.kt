package com.weatherxm.ui.token

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
data class UITransactions(
    var uiTransactions: List<UITransaction>,
    var hasNextPage: Boolean = false,
    var reachedTotal: Boolean = false
)

@Keep
@JsonClass(generateAdapter = true)
data class UITransaction(
    val formattedDate: String,
    val formattedTimestamp: String,
    val txHash: String?,
    val txHashMasked: String?,
    val validationScore: Float?,
    val dailyReward: Float?,
    val actualReward: Float?,
)
