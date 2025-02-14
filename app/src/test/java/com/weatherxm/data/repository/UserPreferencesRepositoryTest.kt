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
        justRun { dataSource.setWalletWarningDismissTimestamp() }
        every { dataSource.getWalletWarningDismissTimestamp() } returns 0
        justRun { dataSource.setDevicesSortFilterOptions(sort, filter, group) }
        justRun { dataSource.setAcceptTerms() }
        justRun { dataSource.setClaimingBadgeShouldShow(any()) }
        every { dataSource.getDevicesSortFilterOptions() } returns sortFilterGroupOptions
        every { dataSource.getClaimingBadgeShouldShow() } returns true
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
            every { dataSource.getAnalyticsDecisionTimestamp() } returns 0
            then("return true") {
                repo.shouldShowAnalyticsOptIn() shouldBe true
            }
        }
        When("we should NOT show it") {
            every { dataSource.getAnalyticsDecisionTimestamp() } returns 1
            then("return false") {
                repo.shouldShowAnalyticsOptIn() shouldBe false
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

    context("Get if we should show the terms prompt") {
        When("we should show it") {
            every { dataSource.getAcceptTermsTimestamp() } returns 0
            then("return true") {
                repo.shouldShowTermsPrompt() shouldBe true
            }
        }
        When("we should NOT show it") {
            every { dataSource.getAcceptTermsTimestamp() } returns 1
            then("return false") {
                repo.shouldShowTermsPrompt() shouldBe false
            }
        }
    }

    context("SET accept terms timestamp") {
        When("Using the Cache Source") {
            repo.setAcceptTerms()
            then("set the timestamp in cache") {
                verify(exactly = 1) { dataSource.setAcceptTerms() }
            }
        }
    }

    context("GET / SET if we should show the badge for the claiming") {
        When("Using the Cache Source") {
            and("GET the shouldShow flag") {
                then("return that flag") {
                    repo.getClaimingBadgeShouldShow() shouldBe true
                }
            }
            and("SET the shouldShow flag") {
                repo.setClaimingBadgeShouldShow(true)
                then("set the shouldShow in cache") {
                    verify(exactly = 1) { dataSource.setClaimingBadgeShouldShow(any()) }
                }
            }
        }
    }
})
