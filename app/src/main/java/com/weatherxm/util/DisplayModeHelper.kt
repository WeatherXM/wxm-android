package com.weatherxm.util

import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.analytics.AnalyticsWrapper

class DisplayModeHelper(
    private val resources: Resources,
    private val sharedPreferences: SharedPreferences,
    private val analyticsWrapper: AnalyticsWrapper
) {

    fun setDisplayMode() {
        val displayModeKey = resources.getString(R.string.key_theme)
        val defaultMode = resources.getString(R.string.system_value)
        val savedValue = sharedPreferences.getString(displayModeKey, defaultMode)
        updateDisplayModeInAnalytics(savedValue)
        setDisplayMode(savedValue)
    }

    fun setDisplayMode(savedValue: String?) {
        updateDisplayModeInAnalytics(savedValue)
        AppCompatDelegate.setDefaultNightMode(
            when (savedValue) {
                resources.getString(R.string.dark_value) -> MODE_NIGHT_YES
                resources.getString(R.string.light_value) -> MODE_NIGHT_NO
                else -> MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }

    private fun updateDisplayModeInAnalytics(newValue: String?) {
        analyticsWrapper.setDisplayMode(
            when (newValue) {
                resources.getString(R.string.dark_value) -> {
                    AnalyticsService.UserProperty.DARK.propertyName
                }
                resources.getString(R.string.light_value) -> {
                    AnalyticsService.UserProperty.LIGHT.propertyName
                }
                else -> AnalyticsService.UserProperty.SYSTEM.propertyName
            }
        )
    }

    fun getDisplayMode(): String {
        val displayModeKey = resources.getString(R.string.key_theme)
        val defaultMode = resources.getString(R.string.system_value)
        return sharedPreferences.getString(displayModeKey, defaultMode) ?: defaultMode
    }

    fun isSystem(): Boolean {
        return getDisplayMode() == resources.getString(R.string.system_value)
    }

    fun isDarkModeEnabled(): Boolean {
        val displayModeKey = resources.getString(R.string.key_theme)
        val defaultMode = resources.getString(R.string.system_value)
        val savedValue = sharedPreferences.getString(displayModeKey, defaultMode)

        val systemNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return when (savedValue.toString()) {
            resources.getString(R.string.dark_value) -> true
            resources.getString(R.string.light_value) -> false
            else -> {
                systemNightMode == Configuration.UI_MODE_NIGHT_YES
            }
        }
    }
}
