@file:Suppress("MatchingDeclarationName")
package com.weatherxm.ui

import androidx.annotation.Keep

@Keep
data class UIError(
    var errorMessage: String,
    var retryFunction: (() -> Unit)?
)
