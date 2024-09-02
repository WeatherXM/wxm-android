package com.weatherxm.usecases

import com.weatherxm.data.Survey
import com.weatherxm.data.repository.SurveyRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify

class SurveyUseCaseTest : BehaviorSpec({
    val repo = mockk<SurveyRepository>()
    val usecase = SurveyUseCaseImpl(repo)

    val survey = mockk<Survey>()
    val surveyId = "surveyId"

    beforeSpec {
        justRun { repo.dismissSurvey(surveyId) }
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
})
