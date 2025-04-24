package com.weatherxm.app

import android.content.Context
import androidx.startup.Initializer
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.services.CacheService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class AnalyticsInitializer : Initializer<Unit>, KoinComponent {
    private val cacheService: CacheService by inject()
    private val analyticsWrapper: AnalyticsWrapper by inject()

    override fun create(context: Context) {
        Timber.d("Basic configuration for Analytics Wrapper")
        val enabled = cacheService.getAnalyticsEnabled()
        Timber.d("Initializing Analytics [enabled=$enabled]")
        analyticsWrapper.bindCacheService(cacheService)
        analyticsWrapper.setAnalyticsEnabled(enabled)
        analyticsWrapper.setUserId(cacheService.getUserId())
        analyticsWrapper.setDevicesSortFilterOptions(cacheService.getDevicesSortFilterOptions())
        analyticsWrapper.setDevicesOwn(cacheService.getDevicesOwn())
        analyticsWrapper.setDevicesFavorite(cacheService.getDevicesFavorite())
        analyticsWrapper.setHasWallet(cacheService.hasWallet())
        analyticsWrapper.setUserProperties()
        return
    }

    override fun dependencies(): List<Class<CrashlyticsInitializer>> {
        return listOf(CrashlyticsInitializer::class.java)
    }
}
