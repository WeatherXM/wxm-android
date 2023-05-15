package com.weatherxm.util

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import com.weatherxm.BuildConfig
import com.weatherxm.data.services.CacheService
import timber.log.Timber

class Analytics(
    private val firebaseAnalytics: FirebaseAnalytics,
    private val cacheService: CacheService
) {
    // Screen Names
    enum class Screen(val screenName: String) {
        SPLASH("Splash Screen"),
        ANALYTICS("Analytics Opt-In Prompt"),
        EXPLORER_LANDING("Explorer (Landing)"),
        EXPLORER("Explorer"),
        CLAIM_M5("Claim M5"),
        CLAIM_HELIUM("Claim Helium"),
        WALLET("Wallet"),
        DELETE_ACCOUNT("Delete Account"),
        DEVICE_ALERTS("Device Alerts"),
        HELIUM_OTA("OTA Update"),
        HISTORY("Device History"),
        DEVICES_LIST("Device List"),
        SETTINGS("App Settings"),
        LOGIN("Login"),
        SIGNUP("Sign Up"),
        PASSWORD_RESET("Password Reset"),
        PROFILE("Account"),
        CURRENT_WEATHER("Device Current Weather"),
        FORECAST("Device Forecast"),
        REWARDS("Device Rewards"),
        STATION_SETTINGS("Device Settings"),
        REWARD_TRANSACTIONS("Device Reward Transactions"),
        APP_UPDATE_PROMPT("App Update Prompt"),
        WIDGET_SELECT_STATION("Widget Station Selection")
    }

    init {
        val enabled = cacheService.getAnalyticsEnabled()
        Timber.d("Initializing Analytics [enabled=$enabled]")
        setAnalyticsEnabled(enabled)
    }

    fun setAnalyticsEnabled(enabled: Boolean = cacheService.getAnalyticsEnabled()) {
        if (BuildConfig.DEBUG) {
            Timber.d("Skipping analytics tracking in DEBUG mode [enabled=$enabled].")
            firebaseAnalytics.setAnalyticsCollectionEnabled(false)
        } else {
            Timber.d("Resetting analytics tracking [enabled=$enabled]")
            firebaseAnalytics.setAnalyticsCollectionEnabled(enabled)
        }
    }

    fun trackScreen(screen: Screen, screenClass: String?) {
        if (cacheService.getAnalyticsEnabled()) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
                param(FirebaseAnalytics.Param.SCREEN_NAME, screen.screenName)
                param(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass ?: "")
            }
        }
    }
}
