package com.weatherxm.data.datasource

import com.weatherxm.data.services.CacheService

interface UserPreferenceDataSource {
    fun shouldShowOnboarding(): Boolean
    fun getAnalyticsDecisionTimestamp(): Long
    fun setAnalyticsEnabled(enabled: Boolean)
    fun getDevicesSortFilterOptions(): List<String>
    fun setDevicesSortFilterOptions(sortOrder: String, filter: String, groupBy: String)
    fun setWalletWarningDismissTimestamp()
    fun getWalletWarningDismissTimestamp(): Long
    fun getAcceptTermsTimestamp(): Long
    fun setAcceptTerms()
    fun getClaimingBadgeShouldShow(): Boolean
    fun setClaimingBadgeShouldShow(shouldShow: Boolean)
}

class UserPreferenceDataSourceImpl(
    private val cacheService: CacheService
) : UserPreferenceDataSource {
    override fun shouldShowOnboarding(): Boolean {
        return cacheService.shouldShowOnboarding().apply {
            if (this) cacheService.disableShouldShowOnboarding()
        }
    }

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

    override fun getClaimingBadgeShouldShow(): Boolean {
        return cacheService.getClaimingBadgeShouldShow()
    }

    override fun setClaimingBadgeShouldShow(shouldShow: Boolean) {
        cacheService.setClaimingBadgeShouldShow(shouldShow)
    }
}
