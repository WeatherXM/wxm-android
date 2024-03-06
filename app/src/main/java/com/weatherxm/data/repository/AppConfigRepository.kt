package com.weatherxm.data.repository

import arrow.core.handleErrorWith
import com.google.firebase.installations.FirebaseInstallations
import com.weatherxm.data.datasource.AppConfigDataSource
import com.weatherxm.data.safeAwait
import timber.log.Timber

interface AppConfigRepository {
    fun shouldUpdate(): Boolean
    fun isUpdateMandatory(): Boolean
    fun getChangelog(): String
    fun setLastRemindedVersion()
    fun getInstallationId(): String?
    fun isMainnetEnabled(): Boolean
    fun getMainnetMessage(): String
}

class AppConfigRepositoryImpl(
    private val appConfigDataSource: AppConfigDataSource,
    private val firebaseInstallations: FirebaseInstallations
) : AppConfigRepository {

    override fun shouldUpdate(): Boolean {
        val lastRemindedVersion = appConfigDataSource.getLastRemindedVersion()
        val lastRemoteVersion = appConfigDataSource.getLastRemoteVersionCode()
        return appConfigDataSource.shouldUpdate() &&
            (lastRemindedVersion < lastRemoteVersion || isUpdateMandatory())
    }

    override fun isUpdateMandatory(): Boolean {
        return appConfigDataSource.isUpdateMandatory()
    }

    override fun getChangelog(): String {
        return appConfigDataSource.getChangelog()
    }

    override fun setLastRemindedVersion() {
        appConfigDataSource.setLastRemindedVersion()
    }

    /**
     * Get the installation id from the cache if exists, or else from Firebase.
     * Return null if both fail.
     */
    override fun getInstallationId(): String? {
        return appConfigDataSource.getInstallationId()
            .handleErrorWith {
                firebaseInstallations.id.safeAwait().onRight {
                    Timber.d("Installation ID: $it")
                    appConfigDataSource.setInstallationId(it)
                }
            }
            .getOrNull()
    }

    override fun isMainnetEnabled(): Boolean {
        return appConfigDataSource.isMainnetEnabled()
    }

    override fun getMainnetMessage(): String {
        return appConfigDataSource.getMainnetMessage()
    }
}
