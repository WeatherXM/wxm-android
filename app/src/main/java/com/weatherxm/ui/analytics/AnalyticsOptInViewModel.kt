package com.weatherxm.ui.analytics

import androidx.lifecycle.ViewModel
import com.weatherxm.usecases.AnalyticsOptInUseCase
import com.weatherxm.util.Analytics
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AnalyticsOptInViewModel : ViewModel(), KoinComponent {
    private val useCase: AnalyticsOptInUseCase by inject()
    private val analytics: Analytics by inject()

    fun setAnalyticsEnabled(enabled: Boolean) {
        useCase.setAnalyticsEnabled(enabled)
        analytics.setAnalyticsEnabled(enabled)
    }
}
