package com.weatherxm.data.datasource

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.weatherxm.data.models.InfoBanner
import com.weatherxm.data.models.Survey
import com.weatherxm.data.services.CacheService
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify

class RemoteBannersDataSourceTest : BehaviorSpec({
    val remoteConfig = mockk<FirebaseRemoteConfig>()
    val cacheService = mockk<CacheService>()
    val datasource = RemoteBannersDataSourceImpl(remoteConfig, cacheService)

    val id = "id"
    val title = "title"
    val message = "message"
    val url = "url"
    val actionLabel = "actionLabel"
    val survey = Survey(
        id = id,
        title = title,
        message = message,
        url = url,
        actionLabel = actionLabel
    )
    val infoBanner = InfoBanner(
        id = id,
        title = title,
        message = message,
        url = url,
        actionLabel = actionLabel,
        showActionButton = true,
        showCloseButton = true
    )

    beforeSpec {
        justRun { cacheService.setLastDismissedSurveyId(id) }
        justRun { cacheService.setLastDismissedInfoBannerId(id) }
        every { remoteConfig.getString(RemoteBannersDataSourceImpl.SURVEY_ID) } returns id
        every { remoteConfig.getString(RemoteBannersDataSourceImpl.SURVEY_TITLE) } returns title
        every { remoteConfig.getString(RemoteBannersDataSourceImpl.SURVEY_MESSAGE) } returns message
        every { remoteConfig.getString(RemoteBannersDataSourceImpl.SURVEY_URL) } returns url
        every {
            remoteConfig.getString(RemoteBannersDataSourceImpl.SURVEY_ACTION_LABEL)
        } returns actionLabel
        every { remoteConfig.getBoolean(RemoteBannersDataSourceImpl.SURVEY_SHOW) } returns true
        every { remoteConfig.getString(RemoteBannersDataSourceImpl.INFO_BANNER_ID) } returns id
        every {
            remoteConfig.getString(RemoteBannersDataSourceImpl.INFO_BANNER_TITLE)
        } returns title
        every {
            remoteConfig.getString(RemoteBannersDataSourceImpl.INFO_BANNER_MESSAGE)
        } returns message
        every { remoteConfig.getString(RemoteBannersDataSourceImpl.INFO_BANNER_URL) } returns url
        every {
            remoteConfig.getString(RemoteBannersDataSourceImpl.INFO_BANNER_ACTION_LABEL)
        } returns actionLabel
        every { remoteConfig.getBoolean(RemoteBannersDataSourceImpl.INFO_BANNER_SHOW) } returns true
        every { remoteConfig.getBoolean(RemoteBannersDataSourceImpl.INFO_BUTTON_SHOW) } returns true
        every {
            remoteConfig.getBoolean(RemoteBannersDataSourceImpl.INFO_BANNER_DISMISSABLE)
        } returns true
        every { cacheService.getLastDismissedSurveyId() } returns null
        every { cacheService.getLastDismissedInfoBannerId() } returns null
    }

    context("Get a survey or null") {
        When("The Survey ID is empty") {
            every { remoteConfig.getString(RemoteBannersDataSourceImpl.SURVEY_ID) } returns ""
            then("return null") {
                datasource.getSurvey() shouldBe null
            }
        }
        When("The SURVEY_SHOW flag is false") {
            every { remoteConfig.getString(RemoteBannersDataSourceImpl.SURVEY_ID) } returns id
            every { remoteConfig.getBoolean(RemoteBannersDataSourceImpl.SURVEY_SHOW) } returns false
            then("return null") {
                datasource.getSurvey() shouldBe null
            }
        }
        When("The last survey's dismissed ID is the same as the current one") {
            every { remoteConfig.getBoolean(RemoteBannersDataSourceImpl.SURVEY_SHOW) } returns true
            every { cacheService.getLastDismissedSurveyId() } returns id
            then("return null") {
                datasource.getSurvey() shouldBe null
            }
        }
        When("None of the above apply and we have a survey to show") {
            every { cacheService.getLastDismissedSurveyId() } returns null
            then("return the Survey") {
                datasource.getSurvey() shouldBe survey
            }
        }
    }

    context("Get an info banner or null") {
        When("The Info Banner ID is empty") {
            every { remoteConfig.getString(RemoteBannersDataSourceImpl.INFO_BANNER_ID) } returns ""
            then("return null") {
                datasource.getInfoBanner() shouldBe null
            }
        }
        When("The INFO_BANNER_SHOW flag is false") {
            every { remoteConfig.getString(RemoteBannersDataSourceImpl.INFO_BANNER_ID) } returns id
            every {
                remoteConfig.getBoolean(RemoteBannersDataSourceImpl.INFO_BANNER_SHOW)
            } returns false
            then("return null") {
                datasource.getInfoBanner() shouldBe null
            }
        }
        When("The last info banner's dismissed ID is the same as the current one") {
            every {
                remoteConfig.getBoolean(RemoteBannersDataSourceImpl.INFO_BANNER_SHOW)
            } returns true
            every { cacheService.getLastDismissedInfoBannerId() } returns id
            then("return null") {
                datasource.getInfoBanner() shouldBe null
            }
        }
        When("None of the above apply and we have an info banner to show") {
            every { cacheService.getLastDismissedInfoBannerId() } returns null
            then("return the InfoBanner") {
                datasource.getInfoBanner() shouldBe infoBanner
            }
        }
    }


    context("Dismiss a survey") {
        given("a survey ID") {
            then("save the last dismissed survey ID in cache") {
                datasource.dismissSurvey(id)
                verify(exactly = 1) { cacheService.setLastDismissedSurveyId(id) }
            }
        }
    }

    context("Dismiss an info banner") {
        given("a info banner ID") {
            then("save the last dismissed info banner ID in cache") {
                datasource.dismissInfoBanner(id)
                verify(exactly = 1) { cacheService.setLastDismissedInfoBannerId(id) }
            }
        }
    }
})
