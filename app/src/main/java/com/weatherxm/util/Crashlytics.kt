package com.weatherxm.util

import android.content.SharedPreferences
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.weatherxm.BuildConfig
import com.weatherxm.data.services.CacheService
import com.weatherxm.data.services.CacheService.Companion.KEY_INSTALLATION_ID
import com.weatherxm.data.services.CacheService.Companion.KEY_USER_ID
import timber.log.Timber

class Crashlytics(
    private val firebaseCrashlytics: FirebaseCrashlytics,
    private val cacheService: CacheService,
    preferences: SharedPreferences,
) {

    private val onPreferencesChanged = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        firebaseCrashlytics.setUserProperties()
    }

    init {
        Timber.d("Basic configuration for Firebase Crashlytics")
        preferences.registerOnSharedPreferenceChangeListener(onPreferencesChanged)
        firebaseCrashlytics.also {
            // Setup crash reporting on RELEASE builds
            if (!BuildConfig.DEBUG) {
                Timber.plant(CrashReportingTree(it))
                Timber.d("Enabled Crashlytics crash reporting")
            } else {
                Timber.d("Crashlytics crash reporting disabled in DEBUG builds")
            }
            it.setUserProperties()
        }
    }

    private fun FirebaseCrashlytics.setUserProperties() {
        setUserId(cacheService.getUserId())
        cacheService.getInstallationId().onRight { installationId ->
            setCustomKey(KEY_INSTALLATION_ID, installationId)
        }
    }
}
