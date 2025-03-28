package com.weatherxm.data.repository

import com.weatherxm.data.datasource.RemoteBannersDataSource
import com.weatherxm.data.models.RemoteBanner
import com.weatherxm.data.models.RemoteBannerType
import com.weatherxm.data.models.Survey
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
    val infoBanner = mockk<RemoteBanner>()
    val bannerId = "bannerId"

    beforeSpec {
        justRun { dataSource.dismissSurvey(surveyId) }
        justRun { dataSource.dismissRemoteBanner(RemoteBannerType.INFO_BANNER, bannerId) }
        justRun { dataSource.dismissRemoteBanner(RemoteBannerType.ANNOUNCEMENT, bannerId) }
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
            every { dataSource.getRemoteBanner(RemoteBannerType.INFO_BANNER) } returns infoBanner
            then("return that info banner") {
                repo.getRemoteBanner(RemoteBannerType.INFO_BANNER) shouldBe infoBanner
            }
        }
        When("there is no info banner available") {
            every { dataSource.getRemoteBanner(RemoteBannerType.ANNOUNCEMENT) } returns null
            then("return null") {
                repo.getRemoteBanner(RemoteBannerType.ANNOUNCEMENT) shouldBe null
            }
        }
    }

    context("Dismiss Info Banner") {
        given("the info banner ID") {
            then("dismiss this info banner with that ID") {
                repo.dismissRemoteBanner(RemoteBannerType.INFO_BANNER, bannerId)
                verify(exactly = 1) {
                    dataSource.dismissRemoteBanner(
                        RemoteBannerType.INFO_BANNER,
                        bannerId
                    )
                }
            }
        }
    }

    context("Dismiss Announcement Banner") {
        given("the announcement banner ID") {
            then("dismiss this announcement banner with that ID") {
                repo.dismissRemoteBanner(RemoteBannerType.ANNOUNCEMENT, bannerId)
                verify(exactly = 1) {
                    dataSource.dismissRemoteBanner(
                        RemoteBannerType.ANNOUNCEMENT,
                        bannerId
                    )
                }
            }
        }
    }
})
