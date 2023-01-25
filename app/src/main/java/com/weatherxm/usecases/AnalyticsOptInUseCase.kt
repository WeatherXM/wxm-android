package com.weatherxm.usecases

import com.weatherxm.data.repository.AppConfigRepository
import org.koin.core.component.KoinComponent

interface AnalyticsOptInUseCase {
    fun setAnalyticsEnabled(enabled: Boolean)
}

class AnalyticsOptInUseCaseImpl(
    private val appConfigRepository: AppConfigRepository
) : AnalyticsOptInUseCase, KoinComponent {

    override fun setAnalyticsEnabled(enabled: Boolean) {
        appConfigRepository.setAnalyticsEnabled(enabled)
    }
}
