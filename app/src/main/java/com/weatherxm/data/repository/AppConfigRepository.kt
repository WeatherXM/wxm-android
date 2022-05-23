package com.weatherxm.data.repository

import com.weatherxm.data.datasource.AppConfigDataSource

interface AppConfigRepository {
    fun shouldUpdate(): Boolean
    fun isUpdateMandatory(): Boolean
    fun getChangelog(): String
    fun setLastRemindedVersion()
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
}
