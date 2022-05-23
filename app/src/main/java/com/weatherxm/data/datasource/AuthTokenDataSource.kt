package com.weatherxm.data.datasource

import android.content.SharedPreferences
import arrow.core.Either
import com.weatherxm.data.network.AuthToken

interface AuthTokenDataSource {
    suspend fun getAuthToken(): Either<Error, AuthToken>
    suspend fun setAuthToken(token: AuthToken)
    suspend fun clear()
}

class AuthTokenDataSourceImpl(private val preferences: SharedPreferences, ) : AuthTokenDataSource {

    companion object {
        const val KEY_ACCESS = "access"
        const val KEY_REFRESH = "refresh"
    }

    override suspend fun getAuthToken(): Either<Error, AuthToken> {
        val access = preferences.getString(KEY_ACCESS, null)
        val refresh = preferences.getString(KEY_REFRESH, null)
        return if (!access.isNullOrEmpty() && !refresh.isNullOrEmpty()) {
            Either.Right(AuthToken(access, refresh))
        } else {
            Either.Left(Error("Invalid auth token"))
        }
    }

    override suspend fun setAuthToken(token: AuthToken) {
        preferences.edit().apply {
            putString(KEY_ACCESS, token.access)
            putString(KEY_REFRESH, token.refresh)
        }.apply()
    }

    override suspend fun clear() {
        preferences.edit().clear().apply()
    }
}
