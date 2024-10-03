package com.weatherxm.data.services

import android.content.SharedPreferences
import androidx.annotation.StringRes
import androidx.collection.ArrayMap
import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import com.mapbox.search.result.SearchSuggestion
import com.weatherxm.R
import com.weatherxm.data.models.CountryInfo
import com.weatherxm.data.models.DataError
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.Location
import com.weatherxm.data.models.User
import com.weatherxm.data.models.WeatherData
import com.weatherxm.data.network.AuthToken
import com.weatherxm.ui.common.empty
import com.weatherxm.util.Resources
import okhttp3.Cache
import java.util.concurrent.TimeUnit

@Suppress("TooManyFunctions")
class CacheService(
    private val preferences: SharedPreferences,
    private val encryptedPreferences: SharedPreferences,
    private val okHttpCache: Cache,
    private val resources: Resources
) {
    companion object {
        const val KEY_ACCESS = "access"
        const val KEY_REFRESH = "refresh"
        const val KEY_LAST_REMINDED_VERSION = "last_reminded_version"
        const val KEY_ANALYTICS_OPT_IN_OR_OUT_TIMESTAMP = "analytics_opt_in_or_out_timestamp"
        const val KEY_USERNAME = "username"
        const val KEY_WALLET_WARNING_DISMISSED_TIMESTAMP = "wallet_warning_dismissed_timestamp"
        const val KEY_CURRENT_WEATHER_WIDGET_IDS = "current_weather_widget_ids"
        const val KEY_DEVICES_SORT = "devices_sort"
        const val KEY_DEVICES_FILTER = "devices_filter"
        const val KEY_DEVICES_GROUP_BY = "devices_group_by"
        const val KEY_DEVICES_OWN = "devices_own"
        const val KEY_HAS_WALLET = "has_wallet"
        const val WIDGET_ID = "widget_id"
        const val KEY_USER_ID = "user_id"
        const val KEY_INSTALLATION_ID = "installation_id"
        const val KEY_DISMISSED_SURVEY_ID = "dismissed_survey_id"
        const val KEY_DISMISSED_INFO_BANNER_ID = "dismissed_info_banner_id"

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

        fun getWidgetFormattedKey(widgetId: Int): String {
            return "${WIDGET_ID}_${widgetId}"
        }
    }

    private var user: User? = null
    private var walletAddress: String? = null
    private var forecasts: ArrayMap<String, TimedForecastData> = ArrayMap()
    private var suggestions: ArrayMap<String, List<SearchSuggestion>> = ArrayMap()
    private var locations: ArrayMap<String, Location> = ArrayMap()
    private var followedStationsIds = listOf<String>()
    private var userStationsIds = listOf<String>()
    private var countriesInfo = listOf<CountryInfo>()

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
        val installationId = preferences.getString(KEY_INSTALLATION_ID, null)
        return if (installationId.isNullOrEmpty()) {
            Either.Left(DataError.CacheMissError)
        } else {
            Either.Right(installationId)
        }
    }

    fun setInstallationId(installationId: String) {
        preferences.edit().putString(KEY_INSTALLATION_ID, installationId).apply()
    }

    fun getLastRemindedVersion(): Int {
        return preferences.getInt(KEY_LAST_REMINDED_VERSION, 0)
    }

    fun setLastRemindedVersion(lastVersion: Int) {
        preferences.edit().putInt(KEY_LAST_REMINDED_VERSION, lastVersion).apply()
    }

    fun setAnalyticsDecisionTimestamp(timestamp: Long) {
        preferences.edit().putLong(KEY_ANALYTICS_OPT_IN_OR_OUT_TIMESTAMP, timestamp).apply()
    }

    fun getAnalyticsDecisionTimestamp(): Long {
        return preferences.getLong(KEY_ANALYTICS_OPT_IN_OR_OUT_TIMESTAMP, 0L)
    }

    fun getAnalyticsEnabled(): Boolean {
        return preferences.getBoolean(resources.getString(KEY_ANALYTICS), false)
    }

    fun setAnalyticsEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(resources.getString(KEY_ANALYTICS), enabled).apply()
    }

    fun getLocationAddress(hexIndex: String): Either<Failure, String> {
        val address = preferences.getString(hexIndex, null)
        return if (address.isNullOrEmpty()) {
            Either.Left(DataError.CacheMissError)
        } else {
            Either.Right(address)
        }
    }

    fun setLocationAddress(hexIndex: String, address: String) {
        preferences.edit().putString(hexIndex, address).apply()
    }

    fun getUserUsername(): Either<Failure, String> {
        val username = preferences.getString(KEY_USERNAME, null)
        return if (username.isNullOrEmpty()) {
            Either.Left(DataError.CacheMissError)
        } else {
            Either.Right(username)
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
        preferences.edit().putBoolean(KEY_HAS_WALLET, true).apply()
    }

    fun hasWallet(): Boolean {
        return preferences.getBoolean(KEY_HAS_WALLET, false)
    }

    fun getUser(): Either<Failure, User> {
        return user?.let { Either.Right(it) } ?: Either.Left(DataError.CacheMissError)
    }

    fun setUser(user: User) {
        preferences.edit().putString(KEY_USER_ID, user.id).apply()
        this.user = user
    }

    fun getUserId(): String {
        return preferences.getString(KEY_USER_ID, null) ?: String.empty()
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

    fun clearForecast() {
        this.forecasts.clear()
    }

    fun getSearchSuggestions(query: String): Either<Failure, List<SearchSuggestion>> {
        return suggestions[query]?.right() ?: DataError.CacheMissError.left()
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

    fun setWalletWarningDismissTimestamp(timestamp: Long) {
        preferences.edit().putLong(KEY_WALLET_WARNING_DISMISSED_TIMESTAMP, timestamp).apply()
    }

    fun getWalletWarningDismissTimestamp(): Long {
        return preferences.getLong(KEY_WALLET_WARNING_DISMISSED_TIMESTAMP, 0L)
    }

    fun setDeviceLastOtaVersion(key: String, lastOtaVersion: String) {
        preferences.edit().putString(key, lastOtaVersion).apply()
    }

    fun getDeviceLastOtaVersion(key: String): Either<Failure, String> {
        val lastOtaVersion = preferences.getString(key, null)
        return if (lastOtaVersion.isNullOrEmpty()) {
            Either.Left(DataError.CacheMissError)
        } else {
            Either.Right(lastOtaVersion)
        }
    }

    fun setDeviceLastOtaTimestamp(key: String, timestamp: Long) {
        preferences.edit().putLong(key, timestamp).apply()
    }

    fun getDeviceLastOtaTimestamp(key: String): Long {
        return preferences.getLong(key, 0L)
    }

    fun getDevicesSortFilterOptions(): List<String> {
        val sortOrder = preferences.getString(KEY_DEVICES_SORT, null)
        val filterType = preferences.getString(KEY_DEVICES_FILTER, null)
        val groupBy = preferences.getString(KEY_DEVICES_GROUP_BY, null)
        return listOfNotNull(sortOrder, filterType, groupBy)
    }

    fun setDevicesSortFilterOptions(sortOrder: String, filter: String, groupBy: String) {
        with(preferences.edit()) {
            putString(KEY_DEVICES_SORT, sortOrder)
            putString(KEY_DEVICES_FILTER, filter)
            putString(KEY_DEVICES_GROUP_BY, groupBy)
        }.apply()
    }

    fun removeDeviceOfWidget(key: String) {
        preferences.edit().remove(key).apply()
    }

    fun setWidgetDevice(key: String, deviceId: String) {
        preferences.edit().putString(key, deviceId).apply()
    }

    fun getWidgetDevice(key: String): Either<Failure, String> {
        val deviceId = preferences.getString(key, String.empty())
        return if (deviceId.isNullOrEmpty()) {
            Either.Left(DataError.CacheMissError)
        } else {
            Either.Right(deviceId)
        }
    }

    fun getWidgetIds(): Either<Failure, List<String>> {
        val ids = preferences.getStringSet(KEY_CURRENT_WEATHER_WIDGET_IDS, setOf())
        return if (ids.isNullOrEmpty()) {
            Either.Left(DataError.CacheMissError)
        } else {
            Either.Right(ids.toList())
        }
    }

    fun setWidgetIds(ids: List<String>) {
        preferences.edit().putStringSet(KEY_CURRENT_WEATHER_WIDGET_IDS, ids.toSet()).apply()
    }

    fun getFollowedDevicesIds(): List<String> {
        return followedStationsIds
    }

    fun setFollowedDevicesIds(ids: List<String>) {
        followedStationsIds = ids
    }

    fun getUserDevicesIds(): List<String> {
        return userStationsIds
    }

    fun setUserDevicesIds(ids: List<String>) {
        userStationsIds = ids
        preferences.edit().putInt(KEY_DEVICES_OWN, ids.size).apply()
    }

    fun getDevicesOwn(): Int {
        return preferences.getInt(KEY_DEVICES_OWN, 0)
    }

    fun getLastDismissedSurveyId(): String? {
        return preferences.getString(KEY_DISMISSED_SURVEY_ID, null)
    }

    fun setLastDismissedSurveyId(surveyId: String) {
        preferences.edit().putString(KEY_DISMISSED_SURVEY_ID, surveyId).apply()
    }

    fun getLastDismissedInfoBannerId(): String? {
        return preferences.getString(KEY_DISMISSED_INFO_BANNER_ID, null)
    }

    fun setLastDismissedInfoBannerId(infoBannerId: String) {
        preferences.edit().putString(KEY_DISMISSED_INFO_BANNER_ID, infoBannerId).apply()
    }

    fun getCountriesInfo(): List<CountryInfo> {
        return countriesInfo
    }

    fun setCountriesInfo(info: List<CountryInfo>?) {
        countriesInfo = info ?: mutableListOf()
    }

    fun clearAll() {
        this.walletAddress = null
        this.user = null
        this.forecasts.clear()
        this.suggestions.clear()
        this.locations.clear()
        this.followedStationsIds = listOf()
        this.userStationsIds = listOf()

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
            preferences.getBoolean(resources.getString(KEY_ANALYTICS), false)
        val savedAnalyticsOptInOrOutTimestamp = preferences.getLong(
            KEY_ANALYTICS_OPT_IN_OR_OUT_TIMESTAMP, 0L
        )
        val widgetIds = preferences.getStringSet(KEY_CURRENT_WEATHER_WIDGET_IDS, setOf())
        val devicesOfWidgets = mutableMapOf<String, String>()
        widgetIds?.forEach {
            val key = getWidgetFormattedKey(it.toInt())
            val deviceOfWidget = preferences.getString(key, null)
            if (deviceOfWidget != null) {
                devicesOfWidgets[key] = deviceOfWidget
            }
        }

        preferences.edit().clear().putString(resources.getString(KEY_THEME), savedTheme)
            .putString(resources.getString(KEY_TEMPERATURE), savedTemperature)
            .putString(resources.getString(KEY_PRECIP), savedPrecipitation)
            .putString(resources.getString(KEY_WIND), savedWind)
            .putString(resources.getString(KEY_WIND_DIR), savedWindDir)
            .putString(resources.getString(KEY_PRESSURE), savedPressure)
            .putBoolean(resources.getString(KEY_ANALYTICS), savedAnalyticsEnabled)
            .putLong(KEY_ANALYTICS_OPT_IN_OR_OUT_TIMESTAMP, savedAnalyticsOptInOrOutTimestamp)
            .putStringSet(KEY_CURRENT_WEATHER_WIDGET_IDS, widgetIds)
            .apply()

        devicesOfWidgets.forEach {
            preferences.edit().putString(it.key, it.value).apply()
        }
    }

    private fun getPreferenceString(@StringRes key: Int, @StringRes fallback: Int): String? {
        return preferences.getString(resources.getString(key), resources.getString(fallback))
    }

    data class TimedForecastData(
        val value: List<WeatherData>,
        private val cacheExpirationTime: Long = DEFAULT_CACHE_EXPIRATION
    ) {
        private val creationTime: Long = now()

        fun isExpired() = now() - creationTime > cacheExpirationTime

        private fun now() = System.currentTimeMillis()
    }
}
