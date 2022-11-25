@file:Suppress("MatchingDeclarationName")

package com.weatherxm.ui.claimdevice.result

import androidx.annotation.Keep
import com.weatherxm.data.Device

@Keep
data class ClaimResult(
    val device: Device? = null,
    val errorCode: String? = null
)
