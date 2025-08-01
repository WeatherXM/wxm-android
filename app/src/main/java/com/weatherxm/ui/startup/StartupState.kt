package com.weatherxm.ui.startup

import androidx.annotation.Keep
import com.weatherxm.data.models.WXMRemoteMessage

@Keep
sealed class StartupState {
    data object ShowHome : StartupState()
    data object ShowAnalyticsOptIn : StartupState()
    data object ShowUpdate : StartupState()
    data class ShowDeepLinkRouter(val remoteMessage: WXMRemoteMessage?) : StartupState()
}
