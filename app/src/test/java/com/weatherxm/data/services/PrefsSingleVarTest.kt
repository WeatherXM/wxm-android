package com.weatherxm.data.services

import android.content.SharedPreferences
import com.weatherxm.TestConfig.sharedPref
import com.weatherxm.data.models.RemoteBannerType
import com.weatherxm.data.services.CacheService.Companion.KEY_ACCEPT_TERMS_TIMESTAMP
import com.weatherxm.data.services.CacheService.Companion.KEY_ANALYTICS_OPT_IN_OR_OUT_TIMESTAMP
import com.weatherxm.data.services.CacheService.Companion.KEY_DISMISSED_ANNOUNCEMENT_ID
import com.weatherxm.data.services.CacheService.Companion.KEY_DISMISSED_INFO_BANNER_ID
import com.weatherxm.data.services.CacheService.Companion.KEY_DISMISSED_SURVEY_ID
import com.weatherxm.data.services.CacheService.Companion.KEY_LAST_REMINDED_VERSION
import com.weatherxm.data.services.CacheService.Companion.KEY_PHOTO_VERIFICATION_ACCEPTED_TERMS
import com.weatherxm.data.services.CacheService.Companion.KEY_SHOULD_SHOW_CLAIMING_BADGE
import com.weatherxm.data.services.CacheService.Companion.KEY_WALLET_WARNING_DISMISSED_TIMESTAMP
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.verify

class PrefsSingleVarTest(
    private val cacheService: CacheService,
    private val prefEditor: SharedPreferences.Editor
) {
    private val timestamp = System.currentTimeMillis()
    private val otaKey = "otaKey"
    private val surveyId = "surveyId"
    private val infoBannerId = "infoBannerId"
    private val announcementBannerId = "announcementBannerId"
    private val lastRemindedVersion = 0

    @Suppress("LongParameterList")
    private fun BehaviorSpec.testGetSetSingleVar(
        dataTitle: String,
        data: Any,
        mockFunction: () -> Any?,
        verifyFunction: () -> Any,
        getFunction: () -> Any?,
        setFunction: () -> Any
    ) {
        context("GET / SET $dataTitle") {
            When("GET the $dataTitle") {
                then("return the $dataTitle") {
                    every { mockFunction() } returns data
                    getFunction() shouldBe data
                }
            }
            When("SET the $dataTitle") {
                then("set the $dataTitle in the Shared Preferences") {
                    setFunction()
                    verify(exactly = 1) { verifyFunction() }
                }
            }
        }
    }

    @Suppress("LongMethod")
    fun test(behaviorSpec: BehaviorSpec) {
        behaviorSpec.testGetSetSingleVar(
            "Last Reminded Version",
            lastRemindedVersion,
            { sharedPref.getInt(KEY_LAST_REMINDED_VERSION, 0) },
            { prefEditor.putInt(KEY_LAST_REMINDED_VERSION, lastRemindedVersion) },
            { cacheService.getLastRemindedVersion() },
            { cacheService.setLastRemindedVersion(lastRemindedVersion) }
        )

        behaviorSpec.testGetSetSingleVar(
            "Analytics Decision Timestamp",
            timestamp,
            { sharedPref.getLong(KEY_ANALYTICS_OPT_IN_OR_OUT_TIMESTAMP, 0) },
            { prefEditor.putLong(KEY_ANALYTICS_OPT_IN_OR_OUT_TIMESTAMP, timestamp) },
            { cacheService.getAnalyticsDecisionTimestamp() },
            { cacheService.setAnalyticsDecisionTimestamp(timestamp) }
        )

        behaviorSpec.testGetSetSingleVar(
            "Accept Terms Timestamp",
            timestamp,
            { sharedPref.getLong(KEY_ACCEPT_TERMS_TIMESTAMP, 0) },
            { prefEditor.putLong(KEY_ACCEPT_TERMS_TIMESTAMP, timestamp) },
            { cacheService.getAcceptTermsTimestamp() },
            { cacheService.setAcceptTermsTimestamp(timestamp) }
        )

        behaviorSpec.testGetSetSingleVar(
            "Analytics Enabled flag",
            true,
            { sharedPref.getBoolean("google_analytics", false) },
            { prefEditor.putBoolean("google_analytics", false) },
            { cacheService.getAnalyticsEnabled() },
            { cacheService.setAnalyticsEnabled(false) }
        )

        behaviorSpec.testGetSetSingleVar(
            "Wallet Warning Dismiss Timestamp",
            timestamp,
            { sharedPref.getLong(KEY_WALLET_WARNING_DISMISSED_TIMESTAMP, 0) },
            { prefEditor.putLong(KEY_WALLET_WARNING_DISMISSED_TIMESTAMP, timestamp) },
            { cacheService.getWalletWarningDismissTimestamp() },
            { cacheService.setWalletWarningDismissTimestamp(timestamp) }
        )

        behaviorSpec.testGetSetSingleVar(
            "Timestamp of the Last OTA version's we showed a notification",
            timestamp,
            { sharedPref.getLong(otaKey, 0) },
            { prefEditor.putLong(otaKey, timestamp) },
            { cacheService.getDeviceLastOtaTimestamp(otaKey) },
            { cacheService.setDeviceLastOtaTimestamp(otaKey, timestamp) }
        )

        behaviorSpec.testGetSetSingleVar(
            "ID of the last dismissed survey",
            surveyId,
            { sharedPref.getString(KEY_DISMISSED_SURVEY_ID, null) },
            { prefEditor.putString(KEY_DISMISSED_SURVEY_ID, surveyId) },
            { cacheService.getLastDismissedSurveyId() },
            { cacheService.setLastDismissedSurveyId(surveyId) }
        )

        behaviorSpec.testGetSetSingleVar(
            "ID of the last dismissed info banner",
            infoBannerId,
            { sharedPref.getString(KEY_DISMISSED_INFO_BANNER_ID, null) },
            { prefEditor.putString(KEY_DISMISSED_INFO_BANNER_ID, infoBannerId) },
            { cacheService.getLastDismissedRemoteBannerId(RemoteBannerType.INFO_BANNER) },
            {
                cacheService.setLastDismissedRemoteBannerId(
                    RemoteBannerType.INFO_BANNER,
                    infoBannerId
                )
            }
        )

        behaviorSpec.testGetSetSingleVar(
            "ID of the last dismissed announcement banner",
            announcementBannerId,
            { sharedPref.getString(KEY_DISMISSED_ANNOUNCEMENT_ID, null) },
            { prefEditor.putString(KEY_DISMISSED_ANNOUNCEMENT_ID, announcementBannerId) },
            { cacheService.getLastDismissedRemoteBannerId(RemoteBannerType.ANNOUNCEMENT) },
            {
                cacheService.setLastDismissedRemoteBannerId(
                    RemoteBannerType.ANNOUNCEMENT,
                    announcementBannerId
                )
            }
        )

        behaviorSpec.testGetSetSingleVar(
            "if the user has accepted the terms of photo verification",
            true,
            { sharedPref.getBoolean(KEY_PHOTO_VERIFICATION_ACCEPTED_TERMS, false) },
            { prefEditor.putBoolean(KEY_PHOTO_VERIFICATION_ACCEPTED_TERMS, true) },
            { cacheService.getPhotoVerificationAcceptedTerms() },
            { cacheService.setPhotoVerificationAcceptedTerms() }
        )

        behaviorSpec.testGetSetSingleVar(
            "if we should show the badge for claiming or not",
            false,
            { sharedPref.getBoolean(KEY_SHOULD_SHOW_CLAIMING_BADGE, true) },
            { prefEditor.putBoolean(KEY_SHOULD_SHOW_CLAIMING_BADGE, false) },
            { cacheService.getClaimingBadgeShouldShow() },
            { cacheService.setClaimingBadgeShouldShow(false) }
        )
    }
}
