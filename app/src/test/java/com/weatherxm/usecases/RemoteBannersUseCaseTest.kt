package com.weatherxm.usecases

import com.weatherxm.data.InfoBanner
import com.weatherxm.data.Survey
import com.weatherxm.data.repository.RemoteBannersRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify

class RemoteBannersUseCaseTest : BehaviorSpec({
    val repo = mockk<RemoteBannersRepository>()
    val usecase = RemoteBannersUseCaseImpl(repo)

    val survey = mockk<Survey>()
    val surveyId = "surveyId"
    val infoBanner = mockk<InfoBanner>()
    val infoBannerId = "infoBannerId"

    beforeSpec {
        justRun { repo.dismissSurvey(surveyId) }
        justRun { repo.dismissInfoBanner(infoBannerId) }
    }

    context("Get survey related information") {
        When("there is a survey available that should be shown") {
            every { repo.getSurvey() } returns survey
            then("return that survey") {
                usecase.getSurvey() shouldBe survey
            }
        }
        When("there is no survey available") {
            every { repo.getSurvey() } returns null
            then("return null") {
                usecase.getSurvey() shouldBe null
            }
        }
    }

    context("Dismiss Survey") {
        given("the survey ID") {
            then("dismiss this survey with that ID") {
                usecase.dismissSurvey(surveyId)
                verify(exactly = 1) { repo.dismissSurvey(surveyId) }
            }
        }
    }

    context("Get info banner related information") {
        When("there is an info banner available that should be shown") {
            every { repo.getInfoBanner() } returns infoBanner
            then("return that info banner") {
                usecase.getInfoBanner() shouldBe infoBanner
            }
        }
        When("there is no info banner available") {
            every { repo.getInfoBanner() } returns null
            then("return null") {
                usecase.getInfoBanner() shouldBe null
            }
        }
    }

    context("Dismiss Info Banner") {
        given("the info banner ID") {
            then("dismiss this info banner with that ID") {
                usecase.dismissInfoBanner(infoBannerId)
                verify(exactly = 1) { repo.dismissInfoBanner(infoBannerId) }
            }
        }
    }
})
