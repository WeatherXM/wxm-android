package com.weatherxm.data.repository

import com.weatherxm.data.datasource.UserPreferenceDataSource
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify

class UserPreferencesRepositoryTest : BehaviorSpec({
    val dataSource = mockk<UserPreferenceDataSource>()
    val repo = UserPreferencesRepositoryImpl(dataSource)

    val sort = "sort"
    val filter = "filter"
    val group = "group"
    val sortFilterGroupOptions = listOf(sort, filter, group)

    beforeSpec {
        justRun { dataSource.setAnalyticsEnabled(any()) }
        justRun { dataSource.dismissSurveyPrompt() }
        justRun { dataSource.setWalletWarningDismissTimestamp() }
        every { dataSource.getWalletWarningDismissTimestamp() } returns 0
        justRun { dataSource.setDevicesSortFilterOptions(sort, filter, group) }
        every { dataSource.getDevicesSortFilterOptions() } returns sortFilterGroupOptions
    }

    context("Enable/Disable analytics") {
        When("analytics should be enabled") {
            then("enable them") {
                repo.setAnalyticsEnabled(true)
                verify(exactly = 1) { dataSource.setAnalyticsEnabled(true) }
            }
        }
        When("analytics should be disabled") {
            then("disabled them") {
                repo.setAnalyticsEnabled(false)
                verify(exactly = 1) { dataSource.setAnalyticsEnabled(false) }
            }
        }
    }

    context("Get if we should show analytics opt-in prompt") {
        When("we should show it") {
            every { dataSource.getAnalyticsOptInTimestamp() } returns 0
            then("return true") {
                repo.shouldShowAnalyticsOptIn() shouldBe true
            }
        }
        When("we should NOT show it") {
            every { dataSource.getAnalyticsOptInTimestamp() } returns 1
            then("return false") {
                repo.shouldShowAnalyticsOptIn() shouldBe false
            }
        }
    }

    context("Get/Set Survey Prompt Dismiss Information") {
        When("we have not dismissed the survey prompt") {
            every { dataSource.hasDismissedSurveyPrompt() } returns false
            then("return false") {
                repo.hasDismissedSurveyPrompt() shouldBe false
            }
        }
        When("we have dismissed the survey prompt") {
            every { dataSource.hasDismissedSurveyPrompt() } returns true
            then("return true") {
                repo.hasDismissedSurveyPrompt() shouldBe true
            }
        }
        When("we dismiss the survey prompt") {
            then("dismiss it") {
                repo.dismissSurveyPrompt()
                verify(exactly = 1) { dataSource.dismissSurveyPrompt() }
            }
        }
    }

    context("Get Wallet Warning Dismiss Information") {
        When("get the timestamp of the wallet warning's dismiss") {
            then("return that timestamp") {
                repo.getWalletWarningDismissTimestamp() shouldBe 0
            }
        }
        When("we dismiss the wallet warning") {
            then("dismiss it") {
                repo.setWalletWarningDismissTimestamp()
                verify(exactly = 1) { dataSource.setWalletWarningDismissTimestamp() }
            }
        }
    }

    context("Get/Set Devices Sort Filter Options") {
        When("get the devices sort filter options") {
            then("return that options") {
                repo.getDevicesSortFilterOptions() shouldBe sortFilterGroupOptions
            }
        }
        When("we set the devices sort filter options") {
            then("set them") {
                repo.setDevicesSortFilterOptions(sort, filter, group)
                verify(exactly = 1) { dataSource.setDevicesSortFilterOptions(sort, filter, group) }
            }
        }
    }
})
