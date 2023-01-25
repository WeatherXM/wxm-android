package com.weatherxm.util

import com.google.firebase.analytics.FirebaseAnalytics
import com.weatherxm.BuildConfig
import com.weatherxm.data.services.CacheService
import timber.log.Timber

class AnalyticsHelper(
    private val firebaseAnalytics: FirebaseAnalytics,
    private val cacheService: CacheService
) {
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
}
