package com.weatherxm.data.datasource

import android.content.SharedPreferences
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.weatherxm.BuildConfig

interface AppConfigDataSource {
    fun shouldUpdate(): Boolean
    fun isUpdateMandatory(): Boolean
    fun getChangelog(): String
    fun getLastRemindedVersion(): Int
    fun setLastRemindedVersion()
    fun getLastRemoteVersionCode(): Int
}

class AppConfigDataSourceImpl(
    private val firebaseRemoteConfig: FirebaseRemoteConfig,
    private val preferences: SharedPreferences
) : AppConfigDataSource {

    companion object {
        const val REMOTE_CONFIG_VERSION_CODE = "android_app_version_code"
        const val REMOTE_CONFIG_MINIMUM_VERSION_CODE = "android_app_minimum_code"
        const val REMOTE_CONFIG_CHANGELOG = "android_app_changelog"
        const val LAST_REMINDED_VERSION = "last_reminded_version"
    }

    override fun getLastRemoteVersionCode(): Int {
        return firebaseRemoteConfig.getDouble(REMOTE_CONFIG_VERSION_CODE).toInt()
    }

    override fun shouldUpdate(): Boolean {
        val latestVerCode = getLastRemoteVersionCode()
        val currentVerCode = BuildConfig.VERSION_CODE

        return currentVerCode < latestVerCode
    }

    override fun isUpdateMandatory(): Boolean {
        val minimumWorkingVerCode =
            firebaseRemoteConfig.getDouble(REMOTE_CONFIG_MINIMUM_VERSION_CODE).toInt()
        val currentVerCode = BuildConfig.VERSION_CODE

        return currentVerCode < minimumWorkingVerCode
    }

    override fun getChangelog(): String {
        return firebaseRemoteConfig.getString(REMOTE_CONFIG_CHANGELOG)
    }

    override fun getLastRemindedVersion(): Int {
        return preferences.getInt(LAST_REMINDED_VERSION, 0)
    }

    override fun setLastRemindedVersion() {
        val lastVersion = firebaseRemoteConfig.getDouble(REMOTE_CONFIG_VERSION_CODE).toInt()
        preferences.edit().putInt(LAST_REMINDED_VERSION, lastVersion).apply()
    }
}
