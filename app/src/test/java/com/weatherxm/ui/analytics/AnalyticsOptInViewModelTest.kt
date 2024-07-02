package com.weatherxm.ui.analytics

import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.usecases.AnalyticsOptInUseCase
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify

class AnalyticsOptInViewModelTest : BehaviorSpec({
    lateinit var viewModel: AnalyticsOptInViewModel
    lateinit var usecase: AnalyticsOptInUseCase
    lateinit var wrapper: AnalyticsWrapper

    beforeTest {
        usecase = mockk<AnalyticsOptInUseCase>()
        wrapper = mockk<AnalyticsWrapper>()
        viewModel = AnalyticsOptInViewModel(usecase, wrapper)
        every { usecase.setAnalyticsEnabled(any()) } just Runs
        every { wrapper.setAnalyticsEnabled(any()) } just Runs
    }

    context("Analytics-related code in Analytics Use Case & Analytics Wrapper is called") {
        When("analytics should be enabled") {
            then("the usecase and the wrapper should get called once") {
                viewModel.setAnalyticsEnabled(true)
                verify(exactly = 0) { usecase.setAnalyticsEnabled(false) }
                verify(exactly = 0) { wrapper.setAnalyticsEnabled(false) }
                verify(exactly = 1) { usecase.setAnalyticsEnabled(true) }
                verify(exactly = 1) { wrapper.setAnalyticsEnabled(true) }
            }
        }
        When("analytics should be disabled") {
            then("the usecase and the wrapper should get called once") {
                viewModel.setAnalyticsEnabled(false)
                verify(exactly = 0) { usecase.setAnalyticsEnabled(true) }
                verify(exactly = 0) { wrapper.setAnalyticsEnabled(true) }
                verify(exactly = 1) { usecase.setAnalyticsEnabled(false) }
                verify(exactly = 1) { wrapper.setAnalyticsEnabled(false) }
            }
        }
    }
})
