package com.weatherxm.data.datasource

import android.content.SharedPreferences
import arrow.core.Either
import com.weatherxm.data.network.Credentials
import org.koin.core.component.KoinComponent

interface CredentialsDataSource {
    suspend fun getCredentials(): Either<Error, Credentials>
    suspend fun setCredentials(credentials: Credentials)
    suspend fun clear()
}

class CredentialsDataSourceImpl(
    private val preferences: SharedPreferences,
) : CredentialsDataSource, KoinComponent {

    companion object {
        const val KEY_USERNAME = "username"
        const val KEY_PASSWORD = "password"
    }

    override suspend fun getCredentials(): Either<Error, Credentials> {
        val username = preferences.getString(KEY_USERNAME, null)
        val password = preferences.getString(KEY_PASSWORD, null)
        return when {
            username.isNullOrEmpty() -> Either.Left(Error("Invalid username"))
            password.isNullOrEmpty() -> Either.Left(Error("Invalid password"))
            else -> Either.Right(Credentials(username, password))
        }
    }

    override suspend fun setCredentials(credentials: Credentials) {
        preferences.edit().apply {
            putString(KEY_USERNAME, credentials.username)
            putString(KEY_PASSWORD, credentials.password)
        }.apply()
    }

    override suspend fun clear() {
        preferences.edit().clear().apply()
    }
}
