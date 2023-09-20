package com.weatherxm.data.datasource

import com.weatherxm.data.services.CacheService

interface UserPreferenceDataSource {
    fun getAnalyticsOptInTimestamp(): Long
    fun setAnalyticsEnabled(enabled: Boolean)
    fun getDevicesSortFilterOptions(): List<String>
    fun setDevicesSortFilterOptions(sortOrder: String, filter: String, groupBy: String)
    fun hasDismissedSurveyPrompt(): Boolean
    fun dismissSurveyPrompt()
    fun setWalletWarningDismissTimestamp()
    fun getWalletWarningDismissTimestamp(): Long
}

class UserPreferenceDataSourceImpl(
    private val cacheService: CacheService
) : UserPreferenceDataSource {
    override fun getAnalyticsOptInTimestamp(): Long {
        return cacheService.getAnalyticsOptInTimestamp()
    }

    override fun setAnalyticsEnabled(enabled: Boolean) {
        cacheService.setAnalyticsEnabled(enabled)
        cacheService.setAnalyticsEnabledTimestamp()
    }

    override fun getDevicesSortFilterOptions(): List<String> {
        return cacheService.getDevicesSortFilterOptions()
    }

    override fun setDevicesSortFilterOptions(sortOrder: String, filter: String, groupBy: String) {
        cacheService.setDevicesSortFilterOptions(sortOrder, filter, groupBy)
    }

    override fun hasDismissedSurveyPrompt(): Boolean {
        return cacheService.hasDismissedSurveyPrompt()
    }

    override fun dismissSurveyPrompt() {
        cacheService.dismissSurveyPrompt()
    }

    override fun setWalletWarningDismissTimestamp() {
        cacheService.setWalletWarningDismissTimestamp()
    }

    override fun getWalletWarningDismissTimestamp(): Long {
        return cacheService.getWalletWarningDismissTimestamp()
    }
}
