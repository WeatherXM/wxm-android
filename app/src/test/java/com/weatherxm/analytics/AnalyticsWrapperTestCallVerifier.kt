package com.weatherxm.analytics

import com.google.firebase.analytics.FirebaseAnalytics
import com.weatherxm.TestConfig.context
import com.weatherxm.util.UnitSelector
import io.kotest.matchers.shouldBe
import io.mockk.verify

class AnalyticsWrapperTestCallVerifier(
    private val service1: AnalyticsService,
    private val service2: AnalyticsService,
    private val analyticsWrapper: AnalyticsWrapper
) {
    fun verifyEmptyUserIdSet(userId: String) {
        verify(exactly = 0) { service1.setUserId(userId) }
        verify(exactly = 0) { service2.setUserId(userId) }
    }

    fun verifyUserIdSet(userId: String) {
        verify(exactly = 1) { service1.setUserId(userId) }
        verify(exactly = 1) { service2.setUserId(userId) }
    }

    fun verifyUserPropertiesSet() {
        verify(exactly = 1) { UnitSelector.getTemperatureUnit(context) }
        verify(exactly = 1) { UnitSelector.getPrecipitationUnit(context, false) }
        verify(exactly = 1) { UnitSelector.getWindUnit(context) }
        verify(exactly = 1) { UnitSelector.getWindDirectionUnit(context) }
        verify(exactly = 1) { UnitSelector.getPressureUnit(context) }
    }

    fun verifyAnalyticsEnabled(enabled: Boolean) {
        analyticsWrapper.getAnalyticsEnabled() shouldBe enabled
        verify(exactly = 1) { service1.setAnalyticsEnabled(enabled) }
        verify(exactly = 1) { service2.setAnalyticsEnabled(enabled) }
    }

    fun verifyOnLogout() {
        verify(exactly = 1) { service1.onLogout() }
        verify(exactly = 1) { service2.onLogout() }
    }

    fun verifyTrackScreen(
        screen: AnalyticsService.Screen,
        arg1: String,
        arg2: String?,
        times: Int
    ) {
        verify(exactly = times) { service1.trackScreen(screen, arg1, arg2) }
        verify(exactly = times) { service2.trackScreen(screen, arg1, arg2) }
    }

    fun verifyTrackEventUserAction(arg1: String, arg2: String?, times: Int) {
        verify(exactly = times) { service1.trackEventUserAction(arg1, arg2) }
        verify(exactly = times) { service2.trackEventUserAction(arg1, arg2) }
    }

    fun verifyTrackEventViewContent(arg1: String, times: Int) {
        verify(exactly = times) { service1.trackEventViewContent(arg1) }
        verify(exactly = times) { service2.trackEventViewContent(arg1) }
    }

    fun verifyTrackEventFailure(failureId: String, times: Int) {
        verify(exactly = times) {
            service1.trackEventViewContent(
                AnalyticsService.ParamValue.FAILURE.paramValue,
                Pair(FirebaseAnalytics.Param.ITEM_ID, failureId)
            )
        }
        verify(exactly = times) {
            service2.trackEventViewContent(
                AnalyticsService.ParamValue.FAILURE.paramValue,
                Pair(FirebaseAnalytics.Param.ITEM_ID, failureId)
            )
        }
    }

    fun verifyTrackEventPrompt(arg1: String, arg2: String, arg3: String, times: Int) {
        verify(exactly = times) { service1.trackEventPrompt(arg1, arg2, arg3) }
        verify(exactly = times) { service2.trackEventPrompt(arg1, arg2, arg3) }
    }

    fun verifyTrackEventSelectContent(arg: String, times: Int) {
        verify(exactly = times) { service1.trackEventSelectContent(arg) }
        verify(exactly = times) { service2.trackEventSelectContent(arg) }
    }
}
