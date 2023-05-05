package com.weatherxm.data.services

import android.content.SharedPreferences
import android.location.Location
import androidx.annotation.StringRes
import androidx.collection.ArrayMap
import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
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
        const val KEY_ANALYTICS_OPT_IN_OR_OUT_TIMESTAMP = "analytics_opt_in_or_out_timestamp"
        const val KEY_USERNAME = "username"
        const val KEY_DISMISSED_SURVEY_PROMPT = "dismissed_survey_prompt"
        const val KEY_WALLET_WARNING_DISMISSED_TIMESTAMP = "wallet_warning_dismissed_timestamp"
        const val KEY_CURRENT_WEATHER_WIDGET_IDS = "current_weather_widget_ids"
        const val WIDGET_ID = "widget_id"
        const val WIDGET_CURRENT_WEATHER_PREFIX = "curr_weather"
        const val WIDGET_CURRENT_WEATHER_TILE_PREFIX = "curr_weather_tile"

        // Default in-memory cache expiration time 15 minutes
        val DEFAULT_CACHE_EXPIRATION = TimeUnit.MINUTES.toMillis(15L)

        // Some Preference's keys
        const val KEY_ANALYTICS = R.string.key_google_analytics
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
    private var installationId: String? = null
    private var forecasts: ArrayMap<String, TimedForecastData> = ArrayMap()
    private var suggestions: ArrayMap<String, List<SearchSuggestion>> = ArrayMap()
    private var locations: ArrayMap<String, Location> = ArrayMap()

    fun getWidgetFormattedKey(widgetId: Int, prefix: String? = null): String {
        return if (prefix != null) {
            "${prefix}_${WIDGET_ID}_${widgetId}"
        } else {
            "${WIDGET_ID}_${widgetId}"
        }
    }

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

    fun getInstallationId(): Either<Failure, String> {
        return installationId?.let { Either.Right(it) } ?: Either.Left(DataError.CacheMissError)
    }

    fun setInstallationId(installationId: String) {
        this.installationId = installationId
    }

    fun getLastRemindedVersion(): Int {
        return preferences.getInt(KEY_LAST_REMINDED_VERSION, 0)
    }

    fun setLastRemindedVersion(lastVersion: Int) {
        preferences.edit().putInt(KEY_LAST_REMINDED_VERSION, lastVersion).apply()
    }

    fun setAnalyticsEnabledTimestamp() {
        preferences.edit()
            .putLong(KEY_ANALYTICS_OPT_IN_OR_OUT_TIMESTAMP, System.currentTimeMillis()).apply()
    }

    fun getAnalyticsOptInTimestamp(): Long {
        return preferences.getLong(KEY_ANALYTICS_OPT_IN_OR_OUT_TIMESTAMP, 0L)
    }

    fun getAnalyticsEnabled(): Boolean {
        return preferences.getBoolean(resHelper.getString(KEY_ANALYTICS), false)
    }

    fun setAnalyticsEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(resHelper.getString(KEY_ANALYTICS), enabled).apply()
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
        return (forecasts[deviceId]?.right() ?: DataError.CacheMissError.left()).flatMap {
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
        return (suggestions[query]?.right() ?: DataError.CacheMissError.left())
    }

    fun setSearchSuggestions(query: String, suggestions: List<SearchSuggestion>) {
        this.suggestions[query] = suggestions
    }

    fun getSuggestionLocation(suggestion: SearchSuggestion): Either<Failure, Location> {
        return locations[suggestion.id]?.right() ?: DataError.CacheMissError.left()
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

    fun setWalletWarningDismissTimestamp() {
        preferences.edit()
            .putLong(KEY_WALLET_WARNING_DISMISSED_TIMESTAMP, System.currentTimeMillis()).apply()
    }

    fun getWalletWarningDismissTimestamp(): Long {
        return preferences.getLong(KEY_WALLET_WARNING_DISMISSED_TIMESTAMP, 0L)
    }

    fun clearForecast() {
        this.forecasts.clear()
    }

    fun setDeviceLastOtaVersion(key: String, lastOtaVersion: String) {
        preferences.edit().putString(key, lastOtaVersion).apply()
    }

    fun getDeviceLastOtaVersion(key: String): Either<Failure, String> {
        val lastOtaVersion = preferences.getString(key, "")
        return when {
            lastOtaVersion.isNullOrEmpty() -> Either.Left(DataError.CacheMissError)
            else -> Either.Right(lastOtaVersion)
        }
    }

    fun setDeviceLastOtaTimestamp(key: String) {
        preferences.edit().putLong(key, System.currentTimeMillis()).apply()
    }

    fun getDeviceLastOtaTimestamp(key: String): Long {
        return preferences.getLong(key, 0L)
    }

    fun setDeviceOfWidget(key: String, deviceId: String) {
        preferences.edit().putString(key, deviceId).apply()
    }

    fun removeDeviceOfWidget(key: String) {
        preferences.edit().remove(key).apply()
    }

    fun getDeviceOfWidget(key: String): Either<Failure, String> {
        val deviceId = preferences.getString(key, "")
        return when {
            deviceId.isNullOrEmpty() -> Either.Left(DataError.CacheMissError)
            else -> Either.Right(deviceId)
        }
    }

    fun setWidgetOfType(widgetId: Int, prefix: String, enabled: Boolean = true) {
        preferences.edit().putBoolean(getWidgetFormattedKey(widgetId, prefix), enabled).apply()
    }

    fun hasWidgetOfType(widgetId: Int, prefix: String): Boolean {
        return preferences.getBoolean(getWidgetFormattedKey(widgetId, prefix), false)
    }

    fun getWidgetIds(): Either<Failure, List<String>> {
        val ids = preferences.getStringSet(KEY_CURRENT_WEATHER_WIDGET_IDS, setOf())
        return when {
            ids.isNullOrEmpty() -> Either.Left(DataError.CacheMissError)
            else -> Either.Right(ids.toList())
        }
    }

    fun setWidgetIds(ids: List<String>) {
        preferences.edit().putStringSet(KEY_CURRENT_WEATHER_WIDGET_IDS, ids.toSet()).apply()
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
        val savedAnalyticsEnabled =
            preferences.getBoolean(resHelper.getString(KEY_ANALYTICS), false)
        val savedAnalyticsOptInOrOutTimestamp = preferences.getLong(
            KEY_ANALYTICS_OPT_IN_OR_OUT_TIMESTAMP, 0L
        )
        val widgetIds = preferences.getStringSet(KEY_CURRENT_WEATHER_WIDGET_IDS, setOf())
        val devicesOfWidgets = mutableMapOf<String, String>()
        val widgetWithTypeKeys = mutableListOf<String>()
        widgetIds?.forEach {
            val key = getWidgetFormattedKey(it.toInt())
            val deviceOfWidget = preferences.getString(key, null)
            if (deviceOfWidget != null) {
                devicesOfWidgets[key] = deviceOfWidget
            }

            getWidgetFormattedKey(it.toInt(), WIDGET_CURRENT_WEATHER_PREFIX).apply {
                if (preferences.getBoolean(this, false)) {
                    widgetWithTypeKeys.add(this)
                }
            }

            getWidgetFormattedKey(it.toInt(), WIDGET_CURRENT_WEATHER_TILE_PREFIX).apply {
                if (preferences.getBoolean(this, false)) {
                    widgetWithTypeKeys.add(this)
                }
            }
        }

        preferences.edit().clear().putString(resHelper.getString(KEY_THEME), savedTheme)
            .putString(resHelper.getString(KEY_TEMPERATURE), savedTemperature)
            .putString(resHelper.getString(KEY_PRECIP), savedPrecipitation)
            .putString(resHelper.getString(KEY_WIND), savedWind)
            .putString(resHelper.getString(KEY_WIND_DIR), savedWindDir)
            .putString(resHelper.getString(KEY_PRESSURE), savedPressure)
            .putBoolean(resHelper.getString(KEY_ANALYTICS), savedAnalyticsEnabled)
            .putLong(KEY_ANALYTICS_OPT_IN_OR_OUT_TIMESTAMP, savedAnalyticsOptInOrOutTimestamp)
            .putStringSet(KEY_CURRENT_WEATHER_WIDGET_IDS, widgetIds)
            .apply()

        devicesOfWidgets.forEach {
            preferences.edit().putString(it.key, it.value).apply()
        }
        widgetWithTypeKeys.forEach {
            preferences.edit().putBoolean(it, true).apply()
        }
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
