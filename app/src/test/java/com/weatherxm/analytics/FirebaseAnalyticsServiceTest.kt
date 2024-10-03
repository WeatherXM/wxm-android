package com.weatherxm.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ParametersBuilder
import com.weatherxm.ui.startup.StartupActivity
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.verify
import org.koin.test.KoinTest

class FirebaseAnalyticsServiceTest : KoinTest, BehaviorSpec({
    val analytics = mockk<FirebaseAnalytics>()
    val service = FirebaseAnalyticsService(analytics)

    val userId = "userId"
    val userProperties = listOf("key" to "value")
    val screen = AnalyticsService.Screen.SPLASH
    val screenClass = StartupActivity::class.java.name
    val screenViewItemId = "screenViewItemId"
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

    beforeSpec {
        justRun { analytics.setUserId(any()) }
        justRun { analytics.setUserProperty("key", "value") }
        justRun { analytics.setAnalyticsCollectionEnabled(any()) }
        justRun { analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, any()) }
        justRun { analytics.logEvent(AnalyticsService.CustomEvent.USER_ACTION.eventName, any()) }
        justRun { analytics.logEvent(AnalyticsService.CustomEvent.VIEW_CONTENT.eventName, any()) }
        justRun { analytics.logEvent(AnalyticsService.CustomEvent.PROMPT.eventName, any()) }
        justRun { analytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, any()) }

        mockkConstructor(Bundle::class)
        mockkConstructor(ParametersBuilder::class)
        justRun { anyConstructed<Bundle>().putString(any(), any()) }
        justRun { anyConstructed<ParametersBuilder>().param(any() as String, any() as String) }
        justRun { anyConstructed<ParametersBuilder>().param(any() as String, any() as Long) }
    }

    context("Set some User Properties") {
        given("a user ID") {
            then("the service should set the user ID") {
                service.setUserId(userId)
                verify(exactly = 1) { analytics.setUserId(userId) }
            }
        }
        given("some other user properties") {
            then("the service should set them") {
                service.setUserProperties(userProperties)
                verify(exactly = 1) { analytics.setUserProperty("key", "value") }
            }
        }
    }

    context("Reset User ID on logout") {
        given("a logout event") {
            then("should set user ID as null") {
                service.onLogout()
                verify(exactly = 1) { analytics.setUserId(null) }
            }
        }
    }

    context("Enable or Disable Analytics") {
        given("a boolean flag indicating if analytics are enabled or not") {
            When("enabled") {
                then("then call setAnalyticsCollectionEnabled with TRUE") {
                    service.setAnalyticsEnabled(true)
                    verify(exactly = 1) { analytics.setAnalyticsCollectionEnabled(true) }
                    verify(exactly = 0) { analytics.setAnalyticsCollectionEnabled(false) }
                }
            }
            When("disabled") {
                then("then call setAnalyticsCollectionEnabled with FALSE") {
                    service.setAnalyticsEnabled(false)
                    verify(exactly = 1) { analytics.setAnalyticsCollectionEnabled(false) }
                }
            }

        }
    }

    context("Track a SCREEN_VIEW event") {
        given("the invocation of trackScreen method") {
            service.trackScreen(screen, screenClass, screenViewItemId)
            then("should log the SCREEN_VIEW event") {
                verify(exactly = 1) {
                    analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, any())
                }
            }
            then("and the SCREEN_VIEW event's params") {
                verify(exactly = 1) {
                    anyConstructed<ParametersBuilder>().param(
                        FirebaseAnalytics.Param.SCREEN_NAME, screen.screenName
                    )
                    anyConstructed<ParametersBuilder>().param(
                        FirebaseAnalytics.Param.SCREEN_CLASS, screenClass
                    )
                    anyConstructed<ParametersBuilder>().param(
                        FirebaseAnalytics.Param.ITEM_ID, screenViewItemId
                    )
                }
            }
        }
    }

    context("Track a USER_ACTION event") {
        given("the invocation of trackEventUserAction method") {
            service.trackEventUserAction(actionName, userActionContentType, userActionCustomParam)
            then("should log the USER_ACTION event") {
                verify(exactly = 1) {
                    analytics.logEvent(AnalyticsService.CustomEvent.USER_ACTION.eventName, any())
                }
            }
            then("and the USER_ACTION event's params") {
                verify(exactly = 1) {
                    anyConstructed<ParametersBuilder>().param(
                        AnalyticsService.CustomParam.ACTION_NAME.paramName, actionName
                    )
                    anyConstructed<ParametersBuilder>().param(
                        FirebaseAnalytics.Param.CONTENT_TYPE, userActionContentType
                    )
                    anyConstructed<ParametersBuilder>().param(
                        userActionCustomParam.first, userActionCustomParam.second
                    )
                }
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
                    analytics.logEvent(AnalyticsService.CustomEvent.VIEW_CONTENT.eventName, any())
                }
            }
            then("and the VIEW_CONTENT event's params") {
                verify(exactly = 1) {
                    anyConstructed<ParametersBuilder>().param(
                        AnalyticsService.CustomParam.CONTENT_NAME.paramName, contentName
                    )
                    anyConstructed<ParametersBuilder>().param(
                        AnalyticsService.CustomParam.CONTENT_ID.paramName, contentId
                    )
                    anyConstructed<ParametersBuilder>().param(
                        viewContentCustomParam.first, viewContentCustomParam.second
                    )
                    anyConstructed<ParametersBuilder>().param(
                        FirebaseAnalytics.Param.SUCCESS, success
                    )
                }
            }
        }
    }

    context("Track a PROMPT event") {
        given("the invocation of trackEventPrompt method") {
            service.trackEventPrompt(promptName, promptType, action, promptCustomParam)
            then("should log the PROMPT event") {
                verify(exactly = 1) {
                    analytics.logEvent(AnalyticsService.CustomEvent.PROMPT.eventName, any())
                }
            }
            then("and the PROMPT event's params") {
                verify(exactly = 1) {
                    anyConstructed<ParametersBuilder>().param(
                        AnalyticsService.CustomParam.PROMPT_NAME.paramName, promptName
                    )
                    anyConstructed<ParametersBuilder>().param(
                        AnalyticsService.CustomParam.PROMPT_TYPE.paramName, promptType
                    )
                    anyConstructed<ParametersBuilder>().param(
                        AnalyticsService.CustomParam.ACTION.paramName, action
                    )
                    anyConstructed<ParametersBuilder>().param(
                        promptCustomParam.first, promptCustomParam.second
                    )
                }
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
                    analytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, any())
                }
            }
            then("and the SELECT_CONTENT event's params") {
                verify(exactly = 1) {
                    anyConstructed<ParametersBuilder>().param(
                        FirebaseAnalytics.Param.CONTENT_TYPE, selectContentContentType
                    )
                    anyConstructed<ParametersBuilder>().param(
                        selectContentCustomParam.first, selectContentCustomParam.second
                    )
                    anyConstructed<ParametersBuilder>().param(FirebaseAnalytics.Param.INDEX, index)
                }
            }
        }
    }
})
