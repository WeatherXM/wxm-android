package com.weatherxm.analytics

import com.weatherxm.util.Weather
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockkObject
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest

class AnalyticsWrapperTest : KoinTest, BehaviorSpec({
    val fakeUserID = "fake123"
    val fakeDisplayMode = "dark"
    val helper = AnalyticsWrapperTestHelper()

    given("an AnalyticsWrapper instance") {
        beforeContainer {
            mockkObject(Weather)
            helper.mockAnalyticsServiceResponses()
        }

        afterContainer {
            stopKoin()
        }

        and("Setting user parameters and tracking events") {
            helper.setupContextStrings()
            helper.analyticsWrapper.setUserId(fakeUserID)
            helper.analyticsWrapper.setDisplayMode(fakeDisplayMode)
            helper.analyticsWrapper.setDevicesSortFilterOptions(
                listOf(
                    "DATE_ADDED",
                    "ALL",
                    "NO_GROUPING"
                )
            )

            and("some predefined user params") {
                then("should set and verify user properties correctly") {
                    helper.analyticsWrapper.getUserId() shouldBe fakeUserID

                    val userProperties = helper.analyticsWrapper.setUserProperties()
                    userProperties.size shouldBe 9
                    userProperties[0] shouldBe ("theme" to fakeDisplayMode)
                    userProperties[1] shouldBe ("UNIT_TEMPERATURE" to "c")
                    userProperties[2] shouldBe ("UNIT_WIND" to "mps")
                    userProperties[3] shouldBe ("UNIT_WIND_DIRECTION" to "card")
                    userProperties[4] shouldBe ("UNIT_PRECIPITATION" to "mm")
                    userProperties[5] shouldBe ("UNIT_PRESSURE" to "hpa")
                    userProperties[6] shouldBe ("SORT_BY" to "date_added")
                    userProperties[7] shouldBe ("FILTER" to "all")
                    userProperties[8] shouldBe ("GROUP_BY" to "no_grouping")

                    helper.verifyUserPropertiesSet()
                }
            }

            and("a boolean flag indicating if analytics are enabled or not") {
                val testArg = "testArg"

                When("disabled") {
                    helper.analyticsWrapper.setAnalyticsEnabled(false)
                    then("should disable analytics and not track any events") {
                        helper.analyticsWrapper.getAnalyticsEnabled() shouldBe false

                        helper.verifyAnalyticsEnabled(false)

                        helper.analyticsWrapper.trackScreen(
                            AnalyticsService.Screen.ANALYTICS,
                            testArg,
                            testArg
                        )
                        helper.verifyTrackScreen(
                            AnalyticsService.Screen.ANALYTICS,
                            testArg,
                            testArg,
                            0
                        )

                        helper.analyticsWrapper.trackEventUserAction(testArg, testArg)
                        helper.verifyTrackEventUserAction(testArg, testArg, 0)

                        helper.analyticsWrapper.trackEventViewContent(testArg, testArg)
                        helper.verifyTrackEventViewContent(testArg, testArg, 0)

                        helper.analyticsWrapper.trackEventPrompt(testArg, testArg, testArg)
                        helper.verifyTrackEventPrompt(testArg, testArg, testArg, 0)

                        helper.analyticsWrapper.trackEventSelectContent(testArg)
                        helper.verifyTrackEventSelectContent(testArg, 0)
                    }
                }

                When("enabled") {
                    helper.analyticsWrapper.setAnalyticsEnabled(true)
                    then("should enable analytics and track various events") {
                        helper.analyticsWrapper.getAnalyticsEnabled() shouldBe true

                        helper.verifyAnalyticsEnabled(true)

                        helper.analyticsWrapper.trackScreen(
                            AnalyticsService.Screen.ANALYTICS,
                            testArg,
                            testArg
                        )
                        helper.verifyTrackScreen(
                            AnalyticsService.Screen.ANALYTICS,
                            testArg,
                            testArg,
                            1
                        )

                        helper.analyticsWrapper.trackEventUserAction(testArg, testArg)
                        helper.verifyTrackEventUserAction(testArg, testArg, 1)

                        helper.analyticsWrapper.trackEventViewContent(testArg, testArg)
                        helper.verifyTrackEventViewContent(testArg, testArg, 1)

                        helper.analyticsWrapper.trackEventPrompt(testArg, testArg, testArg)
                        helper.verifyTrackEventPrompt(testArg, testArg, testArg, 1)

                        helper.analyticsWrapper.trackEventSelectContent(testArg)
                        helper.verifyTrackEventSelectContent(testArg, 1)
                    }
                }
            }
        }
    }

    afterSpec {
        helper.stopKoinModules()
    }
})
