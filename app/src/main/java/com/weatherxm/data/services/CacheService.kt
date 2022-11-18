package com.weatherxm.data.services

import android.content.SharedPreferences
import android.location.Location
import androidx.collection.ArrayMap
import arrow.core.Either
import arrow.core.flatMap
import arrow.core.rightIfNotNull
import com.mapbox.search.result.SearchSuggestion
import com.weatherxm.data.DataError
import com.weatherxm.data.Failure
import com.weatherxm.data.User
import com.weatherxm.data.WeatherData
import com.weatherxm.data.network.AuthToken
import okhttp3.Cache
import java.util.concurrent.TimeUnit

@Suppress("TooManyFunctions")
class CacheService(
    private val preferences: SharedPreferences,
    private val encryptedPreferences: SharedPreferences,
    private val okHttpCache: Cache
) {
    companion object {
        const val KEY_ACCESS = "access"
        const val KEY_REFRESH = "refresh"
        const val KEY_LAST_REMINDED_VERSION = "last_reminded_version"
        const val KEY_USERNAME = "username"
        const val KEY_DISMISSED_SURVEY_PROMPT = "dismissed_survey_prompt"

        // Default in-memory cache expiration time 15 minutes
        val DEFAULT_CACHE_EXPIRATION = TimeUnit.MINUTES.toMillis(15L)
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
        preferences.edit().clear().apply()
        encryptedPreferences.edit().clear().apply()
        okHttpCache.evictAll()
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
