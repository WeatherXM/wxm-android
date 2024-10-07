package com.weatherxm.data.repository

import com.weatherxm.data.datasource.UserPreferenceDataSource

interface UserPreferencesRepository {
    fun shouldShowAnalyticsOptIn(): Boolean
    fun setAnalyticsEnabled(enabled: Boolean)
    fun getWalletWarningDismissTimestamp(): Long
    fun setWalletWarningDismissTimestamp()
    fun getDevicesSortFilterOptions(): List<String>
    fun setDevicesSortFilterOptions(sortOrder: String, filter: String, groupBy: String)
}

class UserPreferencesRepositoryImpl(
    private val datasource: UserPreferenceDataSource
) : UserPreferencesRepository {

    override fun shouldShowAnalyticsOptIn(): Boolean {
        return datasource.getAnalyticsDecisionTimestamp() == 0L
    }

    override fun setAnalyticsEnabled(enabled: Boolean) {
        datasource.setAnalyticsEnabled(enabled)
    }

    override fun setWalletWarningDismissTimestamp() {
        datasource.setWalletWarningDismissTimestamp()
    }

    override fun getWalletWarningDismissTimestamp(): Long {
        return datasource.getWalletWarningDismissTimestamp()
    }

    override fun getDevicesSortFilterOptions(): List<String> {
        return datasource.getDevicesSortFilterOptions()
    }

    override fun setDevicesSortFilterOptions(sortOrder: String, filter: String, groupBy: String) {
        datasource.setDevicesSortFilterOptions(sortOrder, filter, groupBy)
    }
}
