package com.weatherxm.data.repository

import com.weatherxm.data.datasource.AppConfigDataSource

interface AppConfigRepository {
    fun shouldUpdate(): Boolean
    fun isUpdateMandatory(): Boolean
    fun getChangelog(): String
    fun setLastRemindedVersion()
    fun getInstallationId(): String?
    fun isMainnetEnabled(): Boolean
    fun getMainnetMessage(): String
    fun getMainnetUrl(): String
}

class AppConfigRepositoryImpl(
    private val appConfigDataSource: AppConfigDataSource
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
    }

    override fun isMainnetEnabled(): Boolean {
        return appConfigDataSource.isMainnetEnabled()
    }

    override fun getMainnetMessage(): String {
        return appConfigDataSource.getMainnetMessage()
    }

    override fun getMainnetUrl(): String {
        return appConfigDataSource.getMainnetUrl()
    }
}
