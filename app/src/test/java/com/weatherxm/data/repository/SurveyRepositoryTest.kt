package com.weatherxm.data.repository

import com.weatherxm.data.Survey
import com.weatherxm.data.datasource.SurveyDataSource
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify

class SurveyRepositoryTest : BehaviorSpec({
    val dataSource = mockk<SurveyDataSource>()
    val repo = SurveyRepositoryImpl(dataSource)

    val survey = mockk<Survey>()
    val surveyId = "surveyId"

    beforeSpec {
        justRun { dataSource.dismissSurvey(surveyId) }
    }

    context("Get survey related information") {
        When("there is a survey available that should be shown") {
            every { dataSource.getSurvey() } returns survey
            then("return that survey") {
                repo.getSurvey() shouldBe survey
            }
        }
        When("there is no survey available") {
            every { dataSource.getSurvey() } returns null
            then("return null") {
                repo.getSurvey() shouldBe null
            }
        }
    }

    context("Dismiss Survey") {
        given("the survey ID") {
            then("dismiss this survey with that ID") {
                repo.dismissSurvey(surveyId)
                verify(exactly = 1) { dataSource.dismissSurvey(surveyId) }
            }
        }
    }
})
