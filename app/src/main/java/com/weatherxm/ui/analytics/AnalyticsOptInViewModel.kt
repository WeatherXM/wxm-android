package com.weatherxm.ui.analytics

import androidx.lifecycle.ViewModel
import com.weatherxm.usecases.AnalyticsOptInUseCase
import com.weatherxm.analytics.AnalyticsWrapper

class AnalyticsOptInViewModel(
    private val useCase: AnalyticsOptInUseCase,
    private val analytics: AnalyticsWrapper
) : ViewModel() {
    fun setAnalyticsEnabled(enabled: Boolean) {
        useCase.setAnalyticsEnabled(enabled)
        analytics.setAnalyticsEnabled(enabled)
    }
}
