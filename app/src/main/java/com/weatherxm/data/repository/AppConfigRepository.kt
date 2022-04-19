package com.weatherxm.data.repository

import com.weatherxm.data.datasource.AppConfigDataSource
import org.koin.core.component.KoinComponent

class AppConfigRepository(private val appConfigDataSource: AppConfigDataSource) : KoinComponent {

    fun shouldUpdate(): Boolean {
        val lastRemindedVersion = appConfigDataSource.getLastRemindedVersion()
        val lastRemoteVersion = appConfigDataSource.getLastRemoteVersionCode()
        return appConfigDataSource.shouldUpdate() && lastRemindedVersion < lastRemoteVersion
    }

    fun isUpdateMandatory(): Boolean {
        return appConfigDataSource.isUpdateMandatory()
    }

    fun getChangelog(): String {
        return appConfigDataSource.getChangelog()
    }

    fun setLastRemindedVersion() {
        appConfigDataSource.setLastRemindedVersion()
    }
}
