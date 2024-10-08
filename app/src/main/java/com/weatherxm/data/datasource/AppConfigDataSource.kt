package com.weatherxm.data.datasource

import arrow.core.handleErrorWith
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.weatherxm.BuildConfig
import com.weatherxm.data.safeAwait
import com.weatherxm.data.services.CacheService
import timber.log.Timber

interface AppConfigDataSource {
    fun shouldUpdate(): Boolean
    fun isUpdateMandatory(): Boolean
    fun getChangelog(): String
    fun getLastRemindedVersion(): Int
    fun setLastRemindedVersion()
    fun getLastRemoteVersionCode(): Int
    fun setInstallationId(installationId: String)
    fun getInstallationId(): String?
}

class AppConfigDataSourceImpl(
    private val firebaseRemoteConfig: FirebaseRemoteConfig,
    private val firebaseInstallations: FirebaseInstallations,
    private val cacheService: CacheService
) : AppConfigDataSource {

    companion object {
        const val REMOTE_CONFIG_VERSION_CODE = "android_app_version_code"
        const val REMOTE_CONFIG_MINIMUM_VERSION_CODE = "android_app_minimum_code"
        const val REMOTE_CONFIG_CHANGELOG = "android_app_changelog"
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

    /**
     * Get the installation id from the cache if exists, or else from Firebase.
     * Return null if both fail.
     */
    override fun getInstallationId(): String? {
        return cacheService.getInstallationId()
            .handleErrorWith {
                firebaseInstallations.id.safeAwait().onRight {
                    Timber.d("Installation ID: $it")
                    setInstallationId(it)
                }
            }
            .getOrNull()
    }
}
