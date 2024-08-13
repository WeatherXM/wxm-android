package com.weatherxm.usecases

import com.weatherxm.data.repository.UserPreferencesRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify

class AnalyticsOptInUseCaseTest : BehaviorSpec({
    lateinit var repo: UserPreferencesRepository
    lateinit var usecase: AnalyticsOptInUseCase

    beforeTest {
        repo = mockk<UserPreferencesRepository>()
        usecase = AnalyticsOptInUseCaseImpl(repo)
        justRun { repo.setAnalyticsEnabled(any()) }
    }

    context("Analytics-related code in User Preferences Repository is called") {
        When("analytics should be enabled") {
            then("the setter in the repository should get called once") {
                usecase.setAnalyticsEnabled(true)
                verify(exactly = 0) { repo.setAnalyticsEnabled(false) }
                verify(exactly = 1) { repo.setAnalyticsEnabled(true) }
            }
        }
        When("analytics should be disabled") {
            then("the setter in the repository should get called once") {
                usecase.setAnalyticsEnabled(false)
                verify(exactly = 0) { repo.setAnalyticsEnabled(true) }
                verify(exactly = 1) { repo.setAnalyticsEnabled(false) }
            }
        }
    }
})
