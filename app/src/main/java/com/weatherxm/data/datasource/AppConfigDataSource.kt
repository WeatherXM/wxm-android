package com.weatherxm.data.datasource

import arrow.core.Either
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.weatherxm.BuildConfig
import com.weatherxm.data.Failure
import com.weatherxm.data.services.CacheService

interface AppConfigDataSource {
    fun shouldUpdate(): Boolean
    fun isUpdateMandatory(): Boolean
    fun getChangelog(): String
    fun getLastRemindedVersion(): Int
    fun setLastRemindedVersion()
    fun getLastRemoteVersionCode(): Int
    fun setInstallationId(installationId: String)
    fun getInstallationId(): Either<Failure, String>
    fun isMainnetEnabled(): Boolean
    fun getMainnetMessage(): String
}

class AppConfigDataSourceImpl(
    private val firebaseRemoteConfig: FirebaseRemoteConfig,
    private val cacheService: CacheService
) : AppConfigDataSource {

    companion object {
        const val REMOTE_CONFIG_VERSION_CODE = "android_app_version_code"
        const val REMOTE_CONFIG_MINIMUM_VERSION_CODE = "android_app_minimum_code"
        const val REMOTE_CONFIG_CHANGELOG = "android_app_changelog"
        const val REMOTE_CONFIG_MAINNET = "feat_mainnet"
        const val REMOTE_CONFIG_MAINNET_MESSAGE = "feat_mainnet_message"
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
        return cacheService.getLastRemindedVersion()
    }

    override fun setLastRemindedVersion() {
        val lastVersion = firebaseRemoteConfig.getDouble(REMOTE_CONFIG_VERSION_CODE).toInt()
        cacheService.setLastRemindedVersion(lastVersion)
    }

    override fun setInstallationId(installationId: String) {
        cacheService.setInstallationId(installationId)
    }

    override fun getInstallationId(): Either<Failure, String> {
        return cacheService.getInstallationId()
    }

    override fun isMainnetEnabled(): Boolean {
        return firebaseRemoteConfig.getBoolean(REMOTE_CONFIG_MAINNET)
    }

    override fun getMainnetMessage(): String {
        return firebaseRemoteConfig.getString(REMOTE_CONFIG_MAINNET_MESSAGE)
    }
}
