package com.weatherxm.ui.analytics

import androidx.lifecycle.ViewModel
import com.weatherxm.usecases.AnalyticsOptInUseCase
import com.weatherxm.analytics.AnalyticsImpl

class AnalyticsOptInViewModel(
    private val useCase: AnalyticsOptInUseCase,
    private val analytics: AnalyticsImpl
) : ViewModel() {
    fun setAnalyticsEnabled(enabled: Boolean) {
        useCase.setAnalyticsEnabled(enabled)
        analytics.setAnalyticsEnabled(enabled)
    }
}
