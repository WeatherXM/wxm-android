package com.weatherxm.ui.analytics

import androidx.lifecycle.ViewModel
import com.weatherxm.usecases.AnalyticsOptInUseCase
import com.weatherxm.util.Analytics

class AnalyticsOptInViewModel(
    private val useCase: AnalyticsOptInUseCase,
    private val analytics: Analytics
) : ViewModel() {
    fun setAnalyticsEnabled(enabled: Boolean) {
        useCase.setAnalyticsEnabled(enabled)
        analytics.setAnalyticsEnabled(enabled)
    }
}
