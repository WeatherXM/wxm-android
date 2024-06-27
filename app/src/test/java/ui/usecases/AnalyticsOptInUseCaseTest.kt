package ui.usecases

import com.weatherxm.data.repository.UserPreferencesRepository
import com.weatherxm.usecases.AnalyticsOptInUseCaseImpl
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify

class AnalyticsOptInUseCaseTest : BehaviorSpec({
    val repo = mockk<UserPreferencesRepository>()
    val usecase = AnalyticsOptInUseCaseImpl(repo)

    beforeSpec {
        every { repo.setAnalyticsEnabled(any()) } just Runs
    }

    context("Analytics-related code in User Preferences Repository is called") {
        When("analytics should be enabled") {
            then("the setter in the repository should get called once") {
                usecase.setAnalyticsEnabled(true)
                verify(exactly = 1) { repo.setAnalyticsEnabled(true) }
            }
        }
        When("analytics should be disabled") {
            then("the setter in the repository should get called once") {
                usecase.setAnalyticsEnabled(false)
                verify(exactly = 1) { repo.setAnalyticsEnabled(false) }
            }
        }
    }
})
