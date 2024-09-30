package com.weatherxm.analytics

import com.mixpanel.android.mpmetrics.MixpanelAPI
import com.weatherxm.ui.startup.StartupActivity
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.koin.test.KoinTest

class MixpanelAnalyticsServiceTest : KoinTest, BehaviorSpec({
    val mixpanelAPI = mockk<MixpanelAPI>()
    val service = spyk(MixpanelAnalyticsService(mixpanelAPI))

    val userId = "userId"
    val userProperties = listOf("key" to "value")
    val screen = AnalyticsService.Screen.SPLASH
    val screenViewItemId = "screenViewItemId"
    val screenClass = StartupActivity::class.java.name
    val actionName = "actionName"
    val userActionCustomParam = "userActionParamKey" to "userActionParamValue"
    val userActionContentType = "userActionContentType"
    val contentName = "contentName"
    val contentId = "contentId"
    val viewContentCustomParam = "viewContentParamKey" to "viewContentParamValue"
    val success = 0L
    val promptName = "promptName"
    val promptType = "promptType"
    val action = "action"
    val promptCustomParam = "promptParamKey" to "promptParamValue"
    val selectContentContentType = "selectContentContentType"
    val index = 0L
    val selectContentCustomParam = "selectContentParamKey" to "selectContentParamValue"

    val screenViewParams = listOf(
        Pair(AnalyticsService.EventKey.SCREEN_NAME.key, screen.screenName),
        Pair(AnalyticsService.EventKey.ITEM_ID.key, screenViewItemId)
    )
    val userActionsParams = listOf(
        Pair(AnalyticsService.CustomParam.ACTION_NAME.paramName, actionName),
        Pair(userActionCustomParam.first, userActionCustomParam.second),
        Pair(AnalyticsService.EventKey.CONTENT_TYPE.key, userActionContentType)
    )
    val viewContentParams = listOf(
        Pair(AnalyticsService.CustomParam.CONTENT_NAME.paramName, contentName),
        Pair(viewContentCustomParam.first, viewContentCustomParam.second),
        Pair(AnalyticsService.CustomParam.CONTENT_ID.paramName, contentId),
        Pair(AnalyticsService.EventKey.SUCCESS.key, success)
    )
    val promptParams = listOf(
        Pair(AnalyticsService.CustomParam.PROMPT_NAME.paramName, promptName),
        Pair(AnalyticsService.CustomParam.PROMPT_TYPE.paramName, promptType),
        Pair(AnalyticsService.CustomParam.ACTION.paramName, action),
        Pair(promptCustomParam.first, promptCustomParam.second)
    )
    val selectContentParams = listOf(
        Pair(AnalyticsService.EventKey.CONTENT_TYPE.key, selectContentContentType),
        Pair(selectContentCustomParam.first, selectContentCustomParam.second),
        Pair(AnalyticsService.EventKey.INDEX.key, index)
    )

    beforeSpec {
        justRun { mixpanelAPI.identify(any()) }
        justRun { mixpanelAPI.people.set(any()) }
        justRun { mixpanelAPI.optInTracking() }
        justRun { mixpanelAPI.optOutTracking() }
        justRun { mixpanelAPI.reset() }
        justRun { mixpanelAPI.track(AnalyticsService.EventKey.SCREEN_VIEW.key, any()) }
        justRun { mixpanelAPI.track(AnalyticsService.CustomEvent.USER_ACTION.eventName, any()) }
        justRun { mixpanelAPI.track(AnalyticsService.CustomEvent.VIEW_CONTENT.eventName, any()) }
        justRun { mixpanelAPI.track(AnalyticsService.CustomEvent.PROMPT.eventName, any()) }
        justRun { mixpanelAPI.track(AnalyticsService.EventKey.SELECT_CONTENT.key, any()) }
    }

    context("Set some User Properties") {
        given("a user ID") {
            then("the service should set the user ID") {
                service.setUserId(userId)
                verify(exactly = 1) { mixpanelAPI.identify(userId) }
            }
        }
        given("some other user properties") {
            then("the service should set them") {
                service.setUserProperties(userProperties)
                verify(exactly = 1) { mixpanelAPI.people.set(any()) }
                verify(exactly = 1) { service.paramsToJSON(userProperties) }
            }
        }
    }

    context("Reset User ID on logout") {
        given("a logout event") {
            then("should set user ID as null") {
                service.onLogout()
                verify(exactly = 1) { mixpanelAPI.reset() }
            }
        }
    }

    context("Enable or Disable Analytics") {
        given("a boolean flag indicating if analytics are enabled or not") {
            When("enabled") {
                then("then call setAnalyticsCollectionEnabled with TRUE") {
                    service.setAnalyticsEnabled(true)
                    verify(exactly = 1) { mixpanelAPI.optInTracking() }
                    verify(exactly = 0) { mixpanelAPI.optOutTracking() }
                }
            }
            When("disabled") {
                then("then call setAnalyticsCollectionEnabled with FALSE") {
                    service.setAnalyticsEnabled(false)
                    verify(exactly = 1) { mixpanelAPI.optOutTracking() }
                }
            }

        }
    }

    context("Track a SCREEN_VIEW event") {
        given("the invocation of trackScreen method") {
            service.trackScreen(screen, screenClass, screenViewItemId)
            then("should log the SCREEN_VIEW event") {
                verify(exactly = 1) {
                    mixpanelAPI.track(AnalyticsService.EventKey.SCREEN_VIEW.key, any())
                }
            }
            then("and the builder of the JSON params of SCREEN_VIEW is called") {
                verify(exactly = 1) { service.paramsToJSON(screenViewParams) }
            }
        }
    }

    context("Track a USER_ACTION event") {
        given("the invocation of trackEventUserAction method") {
            service.trackEventUserAction(actionName, userActionContentType, userActionCustomParam)
            then("should log the USER_ACTION event") {
                verify(exactly = 1) {
                    mixpanelAPI.track(AnalyticsService.CustomEvent.USER_ACTION.eventName, any())
                }
            }
            then("and the builder of the JSON params of USER_ACTION is called") {
                verify(exactly = 1) { service.paramsToJSON(userActionsParams) }
            }
        }
    }

    context("Track a VIEW_CONTENT event") {
        given("the invocation of trackEventViewContent method") {
            service.trackEventViewContent(
                contentName,
                contentId,
                viewContentCustomParam,
                success = success
            )
            then("should log the VIEW_CONTENT event") {
                verify(exactly = 1) {
                    mixpanelAPI.track(AnalyticsService.CustomEvent.VIEW_CONTENT.eventName, any())
                }
            }
            then("and the builder of the JSON params of VIEW_CONTENT is called") {
                verify(exactly = 1) { service.paramsToJSON(viewContentParams) }
            }
        }
    }

    context("Track a PROMPT event") {
        given("the invocation of trackEventPrompt method") {
            service.trackEventPrompt(promptName, promptType, action, promptCustomParam)
            then("should log the PROMPT event") {
                verify(exactly = 1) {
                    mixpanelAPI.track(AnalyticsService.CustomEvent.PROMPT.eventName, any())
                }
            }
            then("and the builder of the JSON params of PROMPT is called") {
                verify(exactly = 1) { service.paramsToJSON(promptParams) }
            }
        }
    }

    context("Track a SELECT_CONTENT event") {
        given("the invocation of trackEventSelectContent method") {
            service.trackEventSelectContent(
                selectContentContentType,
                selectContentCustomParam,
                index = index
            )
            then("should log the SELECT_CONTENT event") {
                verify(exactly = 1) {
                    mixpanelAPI.track(AnalyticsService.EventKey.SELECT_CONTENT.key, any())
                }
            }
            then("and the builder of the JSON params of SELECT_CONTENT is called") {
                verify(exactly = 1) { service.paramsToJSON(selectContentParams) }
            }
        }
    }
})
