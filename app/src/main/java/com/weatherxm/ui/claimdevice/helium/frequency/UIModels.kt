package com.weatherxm.ui.claimdevice.helium.frequency

import androidx.annotation.Keep

@Keep
data class FrequencyState(
    val country: String?,
    val recommendedFrequency: String,
    val otherFrequencies: List<String>,
)
