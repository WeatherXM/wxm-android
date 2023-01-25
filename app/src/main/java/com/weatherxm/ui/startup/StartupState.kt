package com.weatherxm.ui.startup

import androidx.annotation.Keep

@Keep
sealed class StartupState {
    object ShowExplorer : StartupState()
    object ShowHome : StartupState()
    object ShowAnalyticsOptIn : StartupState()
    object ShowUpdate : StartupState()
}
