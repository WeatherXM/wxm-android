package com.weatherxm.util

import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import com.weatherxm.R

class DisplayModeHelper(
    private val resources: Resources,
    private val sharedPreferences: SharedPreferences
) {

    fun setDisplayMode() {
        val displayModeKey = resources.getString(R.string.key_theme)
        val defaultMode = resources.getString(R.string.system_value)
        val savedValue = sharedPreferences.getString(displayModeKey, defaultMode)

        AppCompatDelegate.setDefaultNightMode(
            when (savedValue.toString()) {
                resources.getString(R.string.dark_value) -> MODE_NIGHT_YES
                resources.getString(R.string.light_value) -> MODE_NIGHT_NO
                resources.getString(R.string.system_value) -> MODE_NIGHT_FOLLOW_SYSTEM
                else -> MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }

    fun setDisplayMode(savedValue: String) {
        AppCompatDelegate.setDefaultNightMode(
            when (savedValue) {
                resources.getString(R.string.dark_value) -> MODE_NIGHT_YES
                resources.getString(R.string.light_value) -> MODE_NIGHT_NO
                resources.getString(R.string.system_value) -> MODE_NIGHT_FOLLOW_SYSTEM
                else -> MODE_NIGHT_FOLLOW_SYSTEM
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
            resources.getString(R.string.system_value) -> {
                systemNightMode == Configuration.UI_MODE_NIGHT_YES
            }
            else -> {
                systemNightMode == Configuration.UI_MODE_NIGHT_YES
            }
        }
    }
}
