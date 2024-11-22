package com.weatherxm.data.datasource

import com.weatherxm.TestConfig.cacheService
import com.weatherxm.ui.common.DevicesFilterType
import com.weatherxm.ui.common.DevicesGroupBy
import com.weatherxm.ui.common.DevicesSortOrder
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coJustRun
import io.mockk.every
import io.mockk.verify

class UserPreferenceDataSourceTest : BehaviorSpec({
    val datasource = UserPreferenceDataSourceImpl(cacheService)

    val enabled = true
    val timestamp = System.currentTimeMillis()
    val sortOrder = DevicesSortOrder.NAME.name
    val filter = DevicesFilterType.ALL.name
    val groupBy = DevicesGroupBy.NO_GROUPING.name
    val filters = listOf(sortOrder, filter, groupBy)

    beforeSpec {
        every { cacheService.getWalletWarningDismissTimestamp() } returns timestamp
        every { cacheService.getAnalyticsDecisionTimestamp() } returns timestamp
        every { cacheService.getDevicesSortFilterOptions() } returns filters
        coJustRun { cacheService.setAnalyticsEnabled(enabled) }
        coJustRun { cacheService.setAnalyticsDecisionTimestamp(any()) }
        coJustRun { cacheService.setWalletWarningDismissTimestamp(any()) }
        coJustRun { cacheService.setDevicesSortFilterOptions(sortOrder, filter, groupBy) }
    }

    context("Get analytics opt-in or opt-out timestamp") {
        When("Using the Cache Source") {
            then("return the timestamp") {
                datasource.getAnalyticsDecisionTimestamp() shouldBe timestamp
            }
        }
    }

    context("Set analytics enabled") {
        When("Using the Cache Source") {
            datasource.setAnalyticsEnabled(enabled)
            then("Set analytics in cache") {
                verify(exactly = 1) { cacheService.setAnalyticsEnabled(true) }
            }
            then("Set analytics timestamp in cache") {
                verify(exactly = 1) { cacheService.setAnalyticsDecisionTimestamp(any()) }
            }
        }
    }

    context("Get user's sort, filter and group-by preferences") {
        When("Using the Cache Source") {
            then("return the preferences as a list") {
                datasource.getDevicesSortFilterOptions() shouldBe filters
            }
        }
    }

    context("Set user's sort, filter and group-by preferences") {
        When("Using the Cache Source") {
            then("Set the preferences in cache") {
                datasource.setDevicesSortFilterOptions(sortOrder, filter, groupBy)
                verify(exactly = 1) {
                    cacheService.setDevicesSortFilterOptions(sortOrder, filter, groupBy)
                }
            }
        }
    }

    context("Get wallet's warning dismiss timestamp") {
        When("Using the Cache Source") {
            then("return the timestamp") {
                datasource.getWalletWarningDismissTimestamp() shouldBe timestamp
            }
        }
    }

    context("Set wallet's warning dismiss timestamp") {
        When("Using the Cache Source") {
            then("set the timestamp in cache") {
                datasource.setWalletWarningDismissTimestamp()
                verify(exactly = 1) { cacheService.setWalletWarningDismissTimestamp(any()) }
            }
        }
    }
})
