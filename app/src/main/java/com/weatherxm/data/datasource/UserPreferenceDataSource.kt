package com.weatherxm.data.datasource

import com.weatherxm.data.services.CacheService

interface UserPreferenceDataSource {
    fun getAnalyticsDecisionTimestamp(): Long
    fun setAnalyticsEnabled(enabled: Boolean)
    fun getDevicesSortFilterOptions(): List<String>
    fun setDevicesSortFilterOptions(sortOrder: String, filter: String, groupBy: String)
    fun setWalletWarningDismissTimestamp()
    fun getWalletWarningDismissTimestamp(): Long
    fun getAcceptTermsTimestamp(): Long
    fun setAcceptTerms()
}

class UserPreferenceDataSourceImpl(
    private val cacheService: CacheService
) : UserPreferenceDataSource {
    override fun getAnalyticsDecisionTimestamp(): Long {
        return cacheService.getAnalyticsDecisionTimestamp()
    }

    override fun setAnalyticsEnabled(enabled: Boolean) {
        cacheService.setAnalyticsEnabled(enabled)
        cacheService.setAnalyticsDecisionTimestamp(System.currentTimeMillis())
    }

    override fun getDevicesSortFilterOptions(): List<String> {
        return cacheService.getDevicesSortFilterOptions()
    }

    override fun setDevicesSortFilterOptions(sortOrder: String, filter: String, groupBy: String) {
        cacheService.setDevicesSortFilterOptions(sortOrder, filter, groupBy)
    }

    override fun setWalletWarningDismissTimestamp() {
        cacheService.setWalletWarningDismissTimestamp(System.currentTimeMillis())
    }

    override fun getWalletWarningDismissTimestamp(): Long {
        return cacheService.getWalletWarningDismissTimestamp()
    }

    override fun getAcceptTermsTimestamp(): Long {
        return cacheService.getAcceptTermsTimestamp()
    }

    override fun setAcceptTerms() {
        cacheService.setAcceptTermsTimestamp(System.currentTimeMillis())
    }
}
