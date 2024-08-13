package com.weatherxm.ui.analytics

import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.usecases.AnalyticsOptInUseCase
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify

class AnalyticsOptInViewModelTest : BehaviorSpec({
    lateinit var viewModel: AnalyticsOptInViewModel
    lateinit var usecase: AnalyticsOptInUseCase
    lateinit var wrapper: AnalyticsWrapper

    fun verifyExpectedCalls(falseExpectedCalls: Int, trueExpectedCalls: Int) {
        verify(exactly = falseExpectedCalls) { usecase.setAnalyticsEnabled(false) }
        verify(exactly = falseExpectedCalls) { wrapper.setAnalyticsEnabled(false) }
        verify(exactly = trueExpectedCalls) { usecase.setAnalyticsEnabled(true) }
        verify(exactly = trueExpectedCalls) { wrapper.setAnalyticsEnabled(true) }
    }

    beforeContainer {
        usecase = mockk<AnalyticsOptInUseCase>()
        wrapper = mockk<AnalyticsWrapper>()
        viewModel = AnalyticsOptInViewModel(usecase, wrapper)
        justRun { usecase.setAnalyticsEnabled(any()) }
        justRun { wrapper.setAnalyticsEnabled(any()) }
    }

    context("Analytics-related code in Analytics Use Case & Analytics Wrapper is called") {
        When("analytics should be enabled") {
            then("the usecase and the wrapper should get called once") {
                viewModel.setAnalyticsEnabled(true)
                verifyExpectedCalls(0, 1)
            }
        }
        When("analytics should be disabled") {
            then("the usecase and the wrapper should get called once") {
                viewModel.setAnalyticsEnabled(false)
                verifyExpectedCalls(1, 0)
            }
        }
    }
})
