package com.weatherxm.data.services

import android.content.SharedPreferences
import android.location.Location
import androidx.annotation.StringRes
import androidx.collection.ArrayMap
import arrow.core.Either
import arrow.core.flatMap
import arrow.core.rightIfNotNull
import com.mapbox.search.result.SearchSuggestion
import com.weatherxm.R
import com.weatherxm.data.DataError
import com.weatherxm.data.Failure
import com.weatherxm.data.User
import com.weatherxm.data.WeatherData
import com.weatherxm.data.network.AuthToken
import com.weatherxm.util.ResourcesHelper
import okhttp3.Cache
import java.util.concurrent.TimeUnit

@Suppress("TooManyFunctions")
class CacheService(
    private val preferences: SharedPreferences,
    private val encryptedPreferences: SharedPreferences,
    private val okHttpCache: Cache,
    private val resHelper: ResourcesHelper
) {
    companion object {
        const val KEY_ACCESS = "access"
        const val KEY_REFRESH = "refresh"
        const val KEY_LAST_REMINDED_VERSION = "last_reminded_version"
        const val KEY_USERNAME = "username"
        const val KEY_DISMISSED_SURVEY_PROMPT = "dismissed_survey_prompt"

        // Default in-memory cache expiration time 15 minutes
        val DEFAULT_CACHE_EXPIRATION = TimeUnit.MINUTES.toMillis(15L)

        // Some Preference's keys
        const val KEY_THEME = R.string.key_theme
        const val KEY_TEMPERATURE = R.string.key_temperature_preference
        const val KEY_PRECIP = R.string.key_precipitation_preference
        const val KEY_WIND = R.string.key_wind_speed_preference
        const val KEY_WIND_DIR = R.string.key_wind_direction_preference
        const val KEY_PRESSURE = R.string.key_pressure_preference
    }

    private var user: User? = null
    private var userId: String = ""
    private var walletAddress: String? = null
    private var forecasts: ArrayMap<String, TimedForecastData> = ArrayMap()
    private var suggestions: ArrayMap<String, List<SearchSuggestion>> = ArrayMap()
    private var locations: ArrayMap<String, Location> = ArrayMap()

    fun getAuthToken(): Either<Failure, AuthToken> {
        val access = encryptedPreferences.getString(KEY_ACCESS, null)
        val refresh = encryptedPreferences.getString(KEY_REFRESH, null)
        return if (!access.isNullOrEmpty() && !refresh.isNullOrEmpty()) {
            Either.Right(AuthToken(access, refresh))
        } else {
            Either.Left(DataError.CacheMissError)
        }
    }

    fun setAuthToken(token: AuthToken) {
        encryptedPreferences.edit().apply {
            putString(KEY_ACCESS, token.access)
            putString(KEY_REFRESH, token.refresh)
        }.apply()
    }

    fun getLastRemindedVersion(): Int {
        return preferences.getInt(KEY_LAST_REMINDED_VERSION, 0)
    }

    fun setLastRemindedVersion(lastVersion: Int) {
        preferences.edit().putInt(KEY_LAST_REMINDED_VERSION, lastVersion).apply()
    }

    fun getLastFriendlyNameChanged(key: String): Long {
        return preferences.getLong(key, 0L)
    }

    fun setLastFriendlyNameChanged(key: String, timestamp: Long) {
        preferences.edit().putLong(key, timestamp).apply()
    }

    fun getLocationAddress(hexIndex: String): Either<Failure, String?> {
        return preferences.getString(hexIndex, null)?.let {
            Either.Right(it)
        } ?: Either.Left(DataError.CacheMissError)
    }

    fun setLocationAddress(hexIndex: String, address: String) {
        preferences.edit().putString(hexIndex, address).apply()
    }

    fun getUserUsername(): Either<Failure, String> {
        val username = preferences.getString(KEY_USERNAME, null)
        return when {
            username.isNullOrEmpty() -> Either.Left(DataError.CacheMissError)
            else -> Either.Right(username)
        }
    }

    fun setUserUsername(username: String) {
        preferences.edit().putString(KEY_USERNAME, username).apply()
    }

    fun getWalletAddress(): Either<Failure, String?> {
        return walletAddress?.let {
            Either.Right(it)
        } ?: Either.Left(DataError.CacheMissError)
    }

    fun setWalletAddress(address: String) {
        this.walletAddress = address
    }

    fun getUser(): Either<Failure, User> {
        return user?.let { Either.Right(it) } ?: Either.Left(DataError.CacheMissError)
    }

    fun setUser(user: User) {
        this.userId = user.id
        this.user = user
    }

    fun getUserId(): String {
        return userId
    }

    fun getForecast(deviceId: String): Either<Failure, List<WeatherData>> {
        return forecasts[deviceId].rightIfNotNull {
            DataError.CacheMissError
        }.flatMap {
            if (it.isExpired()) {
                Either.Left(DataError.CacheExpiredError)
            } else {
                Either.Right(it.value)
            }
        }
    }

    fun setForecast(deviceId: String, forecast: List<WeatherData>) {
        this.forecasts[deviceId] = TimedForecastData(forecast)
    }

    fun getSearchSuggestions(query: String): Either<Failure, List<SearchSuggestion>> {
        return suggestions[query].rightIfNotNull {
            DataError.CacheMissError
        }.flatMap {
            Either.Right(it)
        }
    }

    fun setSearchSuggestions(query: String, suggestions: List<SearchSuggestion>) {
        this.suggestions[query] = suggestions
    }

    fun getSuggestionLocation(suggestion: SearchSuggestion): Either<Failure, Location> {
        return locations[suggestion.id].rightIfNotNull {
            DataError.CacheMissError
        }.flatMap {
            Either.Right(it)
        }
    }

    fun setSuggestionLocation(suggestion: SearchSuggestion, location: Location) {
        locations[suggestion.id] = location
    }

    fun hasDismissedSurveyPrompt(): Boolean {
        return preferences.getBoolean(KEY_DISMISSED_SURVEY_PROMPT, false)
    }

    fun dismissSurveyPrompt() {
        preferences.edit().putBoolean(KEY_DISMISSED_SURVEY_PROMPT, true).apply()
    }

    fun clearForecast() {
        this.forecasts.clear()
    }

    fun clearAll() {
        this.walletAddress = null
        this.user = null
        this.forecasts.clear()
        this.suggestions.clear()
        this.locations.clear()
        okHttpCache.evictAll()
        encryptedPreferences.edit().clear().apply()
        clearUserPreferences()
    }

    /**
     * Some settings should not be cleared when `clearAll` is being used (like on logging out).
     * They are not private information and they have a serious impact in UX if we reset them.
     * So, here we:
     * 1. Fetch them
     * 2. Save them in temp variables, and
     * 3. Re-save them in the shared preferences.
     */
    private fun clearUserPreferences() {
        val savedTheme = getPreferenceString(KEY_THEME, R.string.system_value)
        val savedTemperature = getPreferenceString(KEY_TEMPERATURE, R.string.temperature_celsius)
        val savedPrecipitation = getPreferenceString(KEY_PRECIP, R.string.precipitation_mm)
        val savedWind = getPreferenceString(KEY_WIND, R.string.wind_speed_ms)
        val savedWindDir = getPreferenceString(KEY_WIND_DIR, R.string.wind_direction_cardinal)
        val savedPressure = getPreferenceString(KEY_PRESSURE, R.string.pressure_hpa)
        preferences.edit()
            .clear()
            .putString(resHelper.getString(KEY_THEME), savedTheme)
            .putString(resHelper.getString(KEY_TEMPERATURE), savedTemperature)
            .putString(resHelper.getString(KEY_PRECIP), savedPrecipitation)
            .putString(resHelper.getString(KEY_WIND), savedWind)
            .putString(resHelper.getString(KEY_WIND_DIR), savedWindDir)
            .putString(resHelper.getString(KEY_PRESSURE), savedPressure)
            .apply()
    }

    private fun getPreferenceString(@StringRes key: Int, @StringRes fallback: Int): String? {
        return preferences.getString(resHelper.getString(key), resHelper.getString(fallback))
    }

    data class TimedForecastData(
        val value: List<WeatherData>,
        private val cacheExpirationTime: Long = DEFAULT_CACHE_EXPIRATION
    ) {
        private val creationTime: Long = now()

        fun isExpired() = (now() - creationTime) > cacheExpirationTime

        private fun now() = System.currentTimeMillis()
    }
}
