package com.weatherxm.data.repository

import com.weatherxm.data.InfoBanner
import com.weatherxm.data.Survey
import com.weatherxm.data.datasource.RemoteBannersDataSource
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify

class RemoteBannersRepositoryTest : BehaviorSpec({
    val dataSource = mockk<RemoteBannersDataSource>()
    val repo = RemoteBannersRepositoryImpl(dataSource)

    val survey = mockk<Survey>()
    val surveyId = "surveyId"
    val infoBanner = mockk<InfoBanner>()
    val infoBannerId = "infoBannerId"

    beforeSpec {
        justRun { dataSource.dismissSurvey(surveyId) }
        justRun { dataSource.dismissInfoBanner(infoBannerId) }
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

    context("Get info banner related information") {
        When("there is an info banner available that should be shown") {
            every { dataSource.getInfoBanner() } returns infoBanner
            then("return that info banner") {
                repo.getInfoBanner() shouldBe infoBanner
            }
        }
        When("there is no info banner available") {
            every { dataSource.getInfoBanner() } returns null
            then("return null") {
                repo.getInfoBanner() shouldBe null
            }
        }
    }

    context("Dismiss Info Banner") {
        given("the info banner ID") {
            then("dismiss this info banner with that ID") {
                repo.dismissInfoBanner(infoBannerId)
                verify(exactly = 1) { dataSource.dismissInfoBanner(infoBannerId) }
            }
        }
    }
})
