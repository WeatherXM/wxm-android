package com.weatherxm.data.services

import android.content.SharedPreferences
import androidx.annotation.StringRes
import androidx.collection.ArrayMap
import androidx.core.content.edit
import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import com.mapbox.search.result.SearchSuggestion
import com.weatherxm.R
import com.weatherxm.data.models.CountryInfo
import com.weatherxm.data.models.DataError
import com.weatherxm.data.models.Device
import com.weatherxm.data.models.DeviceNotificationType
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.Location
import com.weatherxm.data.models.RemoteBannerType
import com.weatherxm.data.models.User
import com.weatherxm.data.models.WeatherData
import com.weatherxm.data.network.AuthToken
import com.weatherxm.ui.common.empty
import com.weatherxm.util.Resources
import net.gotev.uploadservice.protocols.multipart.MultipartUploadRequest
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
        const val KEY_DEVICES_FAVORITE = "devices_favorite"
        const val KEY_HAS_WALLET = "has_wallet"
        const val WIDGET_ID = "widget_id"
        const val KEY_USER_ID = "user_id"
        const val KEY_INSTALLATION_ID = "installation_id"
        const val KEY_DISMISSED_SURVEY_ID = "dismissed_survey_id"
        const val KEY_DISMISSED_INFO_BANNER_ID = "dismissed_info_banner_id"
        const val KEY_DISMISSED_ANNOUNCEMENT_ID = "dismissed_announcement_id"
        const val KEY_ACCEPT_TERMS_TIMESTAMP = "accept_terms_timestamp"
        const val KEY_PHOTO_VERIFICATION_ACCEPTED_TERMS = "photo_verification_accepted_terms"
        const val KEY_SHOULD_SHOW_CLAIMING_BADGE = "should_show_claiming_badge"
        const val KEY_DEVICE_NOTIFICATIONS = "device_notifications"
        const val KEY_DEVICE_NOTIFICATION_TYPES = "device_notification_types"
        const val KEY_DEVICE_NOTIFICATIONS_PROMPT = "device_notifications_prompt"
        const val KEY_DEVICE_NOTIFICATION = "device_notification"
        const val KEY_SAVED_LOCATIONS = "saved_locations"

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

        fun getUserDeviceOwnFormattedKey(deviceBundleName: String): String {
            return "${KEY_DEVICES_OWN}_${deviceBundleName}"
        }

        fun getDeviceNotificationsFormattedKey(deviceId: String): String {
            return "${KEY_DEVICE_NOTIFICATIONS}_${deviceId}"
        }

        fun getDeviceNotificationTypesFormattedKey(deviceId: String): String {
            return "${KEY_DEVICE_NOTIFICATION_TYPES}_${deviceId}"
        }

        fun getDeviceNotificationFormattedKey(
            deviceId: String,
            deviceNotificationType: DeviceNotificationType
        ): String {
            return "${KEY_DEVICE_NOTIFICATION}_${deviceNotificationType}_${deviceId}"
        }
    }

    private var user: User? = null
    private var walletAddress: String? = null
    private var deviceForecasts: ArrayMap<String, TimedForecastData> = ArrayMap()
    private var locationForecasts: ArrayMap<String, TimedForecastData> = ArrayMap()
    private var suggestions: ArrayMap<String, List<SearchSuggestion>> = ArrayMap()
    private var locations: ArrayMap<String, Location> = ArrayMap()
    private var devicePhotoUploadIds: ArrayMap<String, MutableList<String>> = ArrayMap()
    private var uploadIdRequest: ArrayMap<String, MultipartUploadRequest> = ArrayMap()
    private var followedStationsIds = listOf<String>()
    private var userDevices = listOf<Device>()
    private var countriesInfo = listOf<CountryInfo>()

    private var onUserPropertiesChangeListener: ((String, Any) -> Unit)? = null

    fun setUserPropertiesChangeListener(listener: (String, Any) -> Unit) {
        onUserPropertiesChangeListener = listener
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
        encryptedPreferences.edit {
            putString(KEY_ACCESS, token.access)
            putString(KEY_REFRESH, token.refresh)
        }
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
        preferences.edit { putString(KEY_INSTALLATION_ID, installationId) }
    }

    fun getLastRemindedVersion(): Int {
        return preferences.getInt(KEY_LAST_REMINDED_VERSION, 0)
    }

    fun setLastRemindedVersion(lastVersion: Int) {
        preferences.edit { putInt(KEY_LAST_REMINDED_VERSION, lastVersion) }
    }

    fun setAnalyticsDecisionTimestamp(timestamp: Long) {
        preferences.edit { putLong(KEY_ANALYTICS_OPT_IN_OR_OUT_TIMESTAMP, timestamp) }
    }

    fun getAnalyticsDecisionTimestamp(): Long {
        return preferences.getLong(KEY_ANALYTICS_OPT_IN_OR_OUT_TIMESTAMP, 0L)
    }

    fun setAcceptTermsTimestamp(timestamp: Long) {
        preferences.edit { putLong(KEY_ACCEPT_TERMS_TIMESTAMP, timestamp) }
    }

    fun getAcceptTermsTimestamp(): Long {
        return preferences.getLong(KEY_ACCEPT_TERMS_TIMESTAMP, 0L)
    }

    fun getAnalyticsEnabled(): Boolean {
        return preferences.getBoolean(resources.getString(KEY_ANALYTICS), false)
    }

    fun setAnalyticsEnabled(enabled: Boolean) {
        preferences.edit { putBoolean(resources.getString(KEY_ANALYTICS), enabled) }
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
        preferences.edit { putString(KEY_USERNAME, username) }
    }

    fun getWalletAddress(): Either<Failure, String?> {
        return walletAddress?.let {
            Either.Right(it)
        } ?: Either.Left(DataError.CacheMissError)
    }

    fun setWalletAddress(address: String) {
        this.walletAddress = address
        preferences.edit { putBoolean(KEY_HAS_WALLET, true) }
        onUserPropertiesChangeListener?.invoke(KEY_HAS_WALLET, true)
    }

    fun hasWallet(): Boolean {
        return preferences.getBoolean(KEY_HAS_WALLET, false)
    }

    fun getUser(): Either<Failure, User> {
        return user?.let { Either.Right(it) } ?: Either.Left(DataError.CacheMissError)
    }

    fun setUser(user: User) {
        preferences.edit { putString(KEY_USER_ID, user.id) }
        onUserPropertiesChangeListener?.invoke(KEY_USER_ID, user.id)
        this.user = user
    }

    fun getUserId(): String {
        return preferences.getString(KEY_USER_ID, null) ?: String.empty()
    }

    fun getDeviceForecast(deviceId: String): Either<Failure, List<WeatherData>> {
        return (deviceForecasts[deviceId]?.right() ?: DataError.CacheMissError.left()).flatMap {
            if (it.isExpired()) {
                Either.Left(DataError.CacheExpiredError)
            } else {
                Either.Right(it.value)
            }
        }
    }

    fun setDeviceForecast(deviceId: String, forecast: List<WeatherData>) {
        this.deviceForecasts[deviceId] = TimedForecastData(forecast)
    }

    fun clearDeviceForecast() {
        this.deviceForecasts.clear()
    }

    fun getLocationForecast(key: String): Either<Failure, List<WeatherData>> {
        return (locationForecasts[key]?.right() ?: DataError.CacheMissError.left()).flatMap {
            if (it.isExpired()) {
                Either.Left(DataError.CacheExpiredError)
            } else {
                Either.Right(it.value)
            }
        }
    }

    fun setLocationForecast(key: String, forecast: List<WeatherData>) {
        this.locationForecasts[key] = TimedForecastData(forecast)
    }

    fun clearLocationForecast() {
        this.locationForecasts.clear()
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

    fun getDevicePhotoUploadIds(deviceId: String): List<String> {
        return devicePhotoUploadIds[deviceId] ?: listOf()
    }

    fun addDevicePhotoUploadId(deviceId: String, uploadId: String) {
        if (this.devicePhotoUploadIds[deviceId] == null) {
            this.devicePhotoUploadIds[deviceId] = mutableListOf(uploadId)
        } else {
            this.devicePhotoUploadIds[deviceId]?.add(uploadId)
        }
    }

    fun removeDevicePhotoUploadId(deviceId: String, uploadId: String) {
        this.devicePhotoUploadIds[deviceId]?.remove(uploadId)
    }

    fun getUploadIdRequest(uploadId: String): MultipartUploadRequest? {
        return uploadIdRequest[uploadId]
    }

    fun setUploadIdRequest(uploadId: String, request: MultipartUploadRequest) {
        uploadIdRequest[uploadId] = request
    }

    fun removeUploadIdRequest(uploadId: String) {
        uploadIdRequest.remove(uploadId)
    }

    fun setWalletWarningDismissTimestamp(timestamp: Long) {
        preferences.edit { putLong(KEY_WALLET_WARNING_DISMISSED_TIMESTAMP, timestamp) }
    }

    fun getWalletWarningDismissTimestamp(): Long {
        return preferences.getLong(KEY_WALLET_WARNING_DISMISSED_TIMESTAMP, 0L)
    }

    fun setDeviceLastOtaVersion(key: String, lastOtaVersion: String) {
        preferences.edit { putString(key, lastOtaVersion) }
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
        preferences.edit { putLong(key, timestamp) }
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
        preferences.edit {
            putString(KEY_DEVICES_SORT, sortOrder)
            putString(KEY_DEVICES_FILTER, filter)
            putString(KEY_DEVICES_GROUP_BY, groupBy)
        }
    }

    fun removeDeviceOfWidget(key: String) {
        preferences.edit { remove(key) }
    }

    fun setWidgetDevice(key: String, deviceId: String) {
        preferences.edit { putString(key, deviceId) }
    }

    fun getWidgetDevice(key: String): Either<Failure, String> {
        val deviceId = preferences.getString(key, null)
        return if (deviceId.isNullOrEmpty()) {
            Either.Left(DataError.CacheMissError)
        } else {
            Either.Right(deviceId)
        }
    }

    fun getWidgetIds(): Either<Failure, List<String>> {
        val ids = preferences.getStringSet(KEY_CURRENT_WEATHER_WIDGET_IDS, null)
        return if (ids.isNullOrEmpty()) {
            Either.Left(DataError.CacheMissError)
        } else {
            Either.Right(ids.toList())
        }
    }

    fun setWidgetIds(ids: List<String>) {
        preferences.edit { putStringSet(KEY_CURRENT_WEATHER_WIDGET_IDS, ids.toSet()) }
    }

    fun getFollowedDevicesIds(): List<String> {
        return followedStationsIds
    }

    fun setFollowedDevicesIds(ids: List<String>) {
        followedStationsIds = ids
        preferences.edit { putInt(KEY_DEVICES_FAVORITE, ids.size) }
        onUserPropertiesChangeListener?.invoke(KEY_DEVICES_FAVORITE, ids.size)
    }

    fun getDevicesFavorite(): Int {
        return preferences.getInt(KEY_DEVICES_FAVORITE, 0)
    }

    fun getUserDevices(): List<Device> {
        return userDevices
    }

    fun setUserDevices(devices: List<Device>) {
        userDevices = devices
        preferences.edit { putInt(KEY_DEVICES_OWN, devices.size) }
        onUserPropertiesChangeListener?.invoke(KEY_DEVICES_OWN, devices.size)
    }

    fun getDevicesOwn(): Int {
        return preferences.getInt(KEY_DEVICES_OWN, 0)
    }

    fun setUserDevicesOfBundle(bundleKey: String, size: Int) {
        preferences.edit { putInt(bundleKey, size) }
        onUserPropertiesChangeListener?.invoke(bundleKey, size)
    }

    fun getUserDevicesOfBundles(): Map<String, Int> {
        return preferences.all.filter {
            it.key.startsWith(KEY_DEVICES_OWN + "_")
        }.mapKeys {
            it.key.removePrefix(KEY_DEVICES_OWN + "_")
        }.mapValues {
            it.value as Int
        }
    }

    fun getLastDismissedSurveyId(): String? {
        return preferences.getString(KEY_DISMISSED_SURVEY_ID, null)
    }

    fun setLastDismissedSurveyId(surveyId: String) {
        preferences.edit { putString(KEY_DISMISSED_SURVEY_ID, surveyId) }
    }

    fun getLastDismissedRemoteBannerId(bannerType: RemoteBannerType): String? {
        return when (bannerType) {
            RemoteBannerType.INFO_BANNER -> preferences.getString(
                KEY_DISMISSED_INFO_BANNER_ID,
                null
            )
            RemoteBannerType.ANNOUNCEMENT -> preferences.getString(
                KEY_DISMISSED_ANNOUNCEMENT_ID,
                null
            )
        }
    }

    fun setLastDismissedRemoteBannerId(bannerType: RemoteBannerType, bannerId: String) {
        preferences.edit {
            when (bannerType) {
                RemoteBannerType.INFO_BANNER -> putString(KEY_DISMISSED_INFO_BANNER_ID, bannerId)
                RemoteBannerType.ANNOUNCEMENT -> putString(KEY_DISMISSED_ANNOUNCEMENT_ID, bannerId)
            }
        }
    }

    fun getPhotoVerificationAcceptedTerms(): Boolean {
        return preferences.getBoolean(KEY_PHOTO_VERIFICATION_ACCEPTED_TERMS, false)
    }

    fun setPhotoVerificationAcceptedTerms() {
        preferences.edit { putBoolean(KEY_PHOTO_VERIFICATION_ACCEPTED_TERMS, true) }
    }

    fun getCountriesInfo(): List<CountryInfo> {
        return countriesInfo
    }

    fun setCountriesInfo(info: List<CountryInfo>?) {
        countriesInfo = info ?: mutableListOf()
    }

    fun getClaimingBadgeShouldShow(): Boolean {
        return preferences.getBoolean(KEY_SHOULD_SHOW_CLAIMING_BADGE, true)
    }

    fun setClaimingBadgeShouldShow(shouldShow: Boolean) {
        preferences.edit { putBoolean(KEY_SHOULD_SHOW_CLAIMING_BADGE, shouldShow) }
    }

    fun setDeviceNotificationsEnabled(key: String, enabled: Boolean) {
        preferences.edit { putBoolean(key, enabled) }
    }

    fun getDeviceNotificationsEnabled(key: String): Boolean {
        return preferences.getBoolean(key, true)
    }

    fun setDeviceNotificationTypesEnabled(key: String, types: Set<String>) {
        preferences.edit { putStringSet(key, types) }
    }

    fun getDeviceNotificationTypesEnabled(key: String): Set<String> {
        /**
         * By default have enabled all types when the user hasn't edited them.
         */
        val allTypes = DeviceNotificationType.entries.map { it.name }.toSet()
        return preferences.getStringSet(key, allTypes) ?: allTypes
    }

    fun checkDeviceNotificationsPrompt() {
        preferences.edit { putBoolean(KEY_DEVICE_NOTIFICATIONS_PROMPT, false) }
    }

    fun getDeviceNotificationsPrompt(): Boolean {
        return preferences.getBoolean(KEY_DEVICE_NOTIFICATIONS_PROMPT, true)
    }

    fun setDeviceNotificationTypeTimestamp(key: String, timestamp: Long) {
        preferences.edit { putLong(key, timestamp) }
    }

    fun getDeviceNotificationTypeTimestamp(key: String): Long {
        return preferences.getLong(key, 0L)
    }

    fun getSavedLocations(): Set<String> {
        return preferences.getStringSet(KEY_SAVED_LOCATIONS, setOf()) ?: setOf()
    }

    fun setSavedLocations(locations: List<String>) {
        preferences.edit { putStringSet(KEY_SAVED_LOCATIONS, locations.toSet()) }
        onUserPropertiesChangeListener?.invoke(KEY_SAVED_LOCATIONS, locations.size)
    }

    fun getPreferredUnit(
        @StringRes unitKeyResId: Int,
        @StringRes defaultUnitResId: Int
    ): String {
        val unitKey = resources.getString(unitKeyResId)
        val defaultUnit = resources.getString(defaultUnitResId)
        return preferences.getString(unitKey, defaultUnit) ?: defaultUnit
    }

    fun clearAll() {
        walletAddress = null
        user = null
        deviceForecasts.clear()
        locationForecasts.clear()
        suggestions.clear()
        devicePhotoUploadIds.clear()
        uploadIdRequest.clear()
        locations.clear()
        followedStationsIds = listOf()
        userDevices = listOf()

        okHttpCache.evictAll()
        encryptedPreferences.edit { clear() }
        clearUserPreferences()
    }

    fun isCacheEmpty(): Boolean {
        return walletAddress == null && user == null && deviceForecasts.isEmpty()
            && suggestions.isEmpty() && locations.isEmpty() && followedStationsIds.isEmpty()
            && userDevices.isEmpty() && devicePhotoUploadIds.isEmpty()
            && uploadIdRequest.isEmpty() && locationForecasts.isEmpty()
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
        val savedAcceptTermsTimestamp = preferences.getLong(KEY_ACCEPT_TERMS_TIMESTAMP, 0L)
        val savedDeviceNotificationsPrompt = getDeviceNotificationsPrompt()
        val widgetIds = preferences.getStringSet(KEY_CURRENT_WEATHER_WIDGET_IDS, setOf())
        val devicesOfWidgets = mutableMapOf<String, String>()
        widgetIds?.forEach {
            val key = getWidgetFormattedKey(it.toInt())
            val deviceOfWidget = preferences.getString(key, null)
            if (deviceOfWidget != null) {
                devicesOfWidgets[key] = deviceOfWidget
            }
        }
        val installationId = getInstallationId().getOrNull()
        val lastDismissedInfoBanner = preferences.getString(KEY_DISMISSED_INFO_BANNER_ID, null)
        val lastDismissedAnnouncementBanner =
            preferences.getString(KEY_DISMISSED_INFO_BANNER_ID, null)
        val savedLocations = getSavedLocations()

        preferences.edit {
            clear()
                .putString(resources.getString(KEY_THEME), savedTheme)
                .putString(resources.getString(KEY_TEMPERATURE), savedTemperature)
                .putString(resources.getString(KEY_PRECIP), savedPrecipitation)
                .putString(resources.getString(KEY_WIND), savedWind)
                .putString(resources.getString(KEY_WIND_DIR), savedWindDir)
                .putString(resources.getString(KEY_PRESSURE), savedPressure)
                .putBoolean(resources.getString(KEY_ANALYTICS), savedAnalyticsEnabled)
                .putLong(KEY_ANALYTICS_OPT_IN_OR_OUT_TIMESTAMP, savedAnalyticsOptInOrOutTimestamp)
                .putLong(KEY_ACCEPT_TERMS_TIMESTAMP, savedAcceptTermsTimestamp)
                .putStringSet(KEY_CURRENT_WEATHER_WIDGET_IDS, widgetIds)
                .putBoolean(KEY_DEVICE_NOTIFICATIONS_PROMPT, savedDeviceNotificationsPrompt)
                .putStringSet(KEY_SAVED_LOCATIONS, savedLocations.toSet())
        }

        installationId?.let { setInstallationId(it) }
        lastDismissedInfoBanner?.let {
            setLastDismissedRemoteBannerId(RemoteBannerType.INFO_BANNER, it)
        }
        lastDismissedAnnouncementBanner?.let {
            setLastDismissedRemoteBannerId(RemoteBannerType.ANNOUNCEMENT, it)
        }

        devicesOfWidgets.forEach {
            preferences.edit { putString(it.key, it.value) }
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
