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
    fun isTokenClaimingEnabled(): Boolean
    fun isPoLEnabled(): Boolean
    fun getRewardsHideAnnotationThreshold(): Long
}

class AppConfigDataSourceImpl(
    private val firebaseRemoteConfig: FirebaseRemoteConfig,
    private val cacheService: CacheService
) : AppConfigDataSource {

    companion object {
        const val REMOTE_CONFIG_VERSION_CODE = "android_app_version_code"
        const val REMOTE_CONFIG_MINIMUM_VERSION_CODE = "android_app_minimum_code"
        const val REMOTE_CONFIG_CHANGELOG = "android_app_changelog"
        const val REMOTE_CONFIG_TOKEN_CLAIMING = "feat_token_claiming"
        const val REMOTE_CONFIG_REWARDS_HIDE_ANNOTATION_THRESHOLD =
            "rewards_hide_annotation_threshold"
        const val REMOTE_CONFIG_REWARDS_HIDE_ANNOTATION_THRESHOLD_DEFAULT = 100L
        const val REMOTE_CONFIG_POL_ENABLED = "feat_pol_enabled"
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

    override fun isTokenClaimingEnabled(): Boolean {
        /**
         * We use the remote config's value otherwise return true, for future-proof reasons.
         * So after we roll out the token claiming we can safely remove the key/value pair in
         * remote config and it older clients will still be working.
         */
        return if (firebaseRemoteConfig.all.containsKey(REMOTE_CONFIG_TOKEN_CLAIMING)) {
            firebaseRemoteConfig.getBoolean(REMOTE_CONFIG_TOKEN_CLAIMING)
        } else {
            true
        }
    }

    override fun isPoLEnabled(): Boolean {
        /**
         * We use the remote config's value otherwise return true, for future-proof reasons.
         * So after we roll out the token claiming we can safely remove the key/value pair in
         * remote config and it older clients will still be working.
         */
        return if (firebaseRemoteConfig.all.containsKey(REMOTE_CONFIG_POL_ENABLED)) {
            firebaseRemoteConfig.getBoolean(REMOTE_CONFIG_POL_ENABLED)
        } else {
            true
        }
    }

    override fun getRewardsHideAnnotationThreshold(): Long {
        return firebaseRemoteConfig
            .all[REMOTE_CONFIG_REWARDS_HIDE_ANNOTATION_THRESHOLD]
            ?.asLong() ?: REMOTE_CONFIG_REWARDS_HIDE_ANNOTATION_THRESHOLD_DEFAULT
    }
}
