package com.weatherxm.ui.startup

import androidx.annotation.Keep
import com.weatherxm.data.WXMRemoteMessage

@Keep
sealed class StartupState {
    data object ShowExplorer : StartupState()
    data object ShowHome : StartupState()
    data object ShowAnalyticsOptIn : StartupState()
    data object ShowUpdate : StartupState()
    data class ShowUrlRouter(val remoteMessage: WXMRemoteMessage) : StartupState()
}
