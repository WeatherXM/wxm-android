package com.weatherxm.data.repository

import android.content.SharedPreferences
import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.User
import com.weatherxm.data.datasource.CacheUserDataSource
import com.weatherxm.data.datasource.NetworkUserDataSource
import timber.log.Timber

interface UserRepository {
    suspend fun getUser(): Either<Failure, User>
    suspend fun clearCache()
    fun hasDismissedSurveyPrompt(): Boolean
    fun dismissSurveyPrompt()
}

class UserRepositoryImpl(
    private val networkUserDataSource: NetworkUserDataSource,
    private val cacheUserDataSource: CacheUserDataSource,
    private val preferences: SharedPreferences
) : UserRepository {
    companion object {
        const val DISMISSED_SURVEY_PROMPT = "dismissed_survey_prompt"
    }

    /**
     * Gets user from cache or network, combining the underlying data sources
     */
    override suspend fun getUser(): Either<Failure, User> {
        return cacheUserDataSource.getUser()
            .tap {
                Timber.d("Got user from cache [${it.email}].")
            }
            .mapLeft {
                return networkUserDataSource.getUser().tap {
                    Timber.d("Got user from network [${it.email}].")
                    cacheUserDataSource.setUser(it)
                }
            }
    }

    override suspend fun clearCache() {
        cacheUserDataSource.clear()
    }

    override fun hasDismissedSurveyPrompt(): Boolean {
        return preferences.getBoolean(DISMISSED_SURVEY_PROMPT, false)
    }

    override fun dismissSurveyPrompt() {
        preferences.edit().putBoolean(DISMISSED_SURVEY_PROMPT, true).apply()
    }
}
