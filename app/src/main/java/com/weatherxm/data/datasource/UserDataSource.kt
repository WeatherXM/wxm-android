package com.weatherxm.data.datasource

import android.content.SharedPreferences
import arrow.core.Either
import com.weatherxm.data.DataError
import com.weatherxm.data.Failure
import com.weatherxm.data.User
import com.weatherxm.data.map
import com.weatherxm.data.network.ApiService

interface UserDataSource {
    suspend fun getUser(): Either<Failure, User>
    suspend fun setUser(user: User)
    suspend fun clear()
}

class NetworkUserDataSource(
    private val apiService: ApiService,
    private val preferences: SharedPreferences
) : UserDataSource {
    override suspend fun getUser(): Either<Failure, User> {
        return apiService.getUser().map()
    }

    override suspend fun setUser(user: User) {
        // No-op
    }

    override suspend fun clear() {
        preferences.edit().clear().apply()
    }
}

/**
 * In-memory user cache. Could be expanded to use SharedPreferences or a different cache.
 */
class CacheUserDataSource : UserDataSource {
    private var user: User? = null

    override suspend fun getUser(): Either<Failure, User> {
        return user?.let { Either.Right(it) } ?: Either.Left(DataError.CacheMissError)
    }

    override suspend fun setUser(user: User) {
        this.user = user
    }

    override suspend fun clear() {
        this.user = null
    }
}
