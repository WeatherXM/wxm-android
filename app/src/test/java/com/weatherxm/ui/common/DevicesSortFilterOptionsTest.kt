package com.weatherxm.ui.common

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class DevicesSortFilterOptionsTest : BehaviorSpec({
    val sortFilterGroupOptions = mockk<DevicesSortFilterOptions>()

    beforeSpec {
        every { sortFilterGroupOptions.getSortAnalyticsValue() } answers { callOriginal() }
        every { sortFilterGroupOptions.getFilterAnalyticsValue() } answers { callOriginal() }
        every { sortFilterGroupOptions.getGroupByAnalyticsValue() } answers { callOriginal() }
    }

    context("Get device's sort analytics value") {
        When("it's DATE_ADDED") {
            every { sortFilterGroupOptions.sortOrder } returns DevicesSortOrder.DATE_ADDED
            then("return DATE_ADDED analytics proper value") {
                sortFilterGroupOptions.getSortAnalyticsValue() shouldBe "date_added"
            }
        }
        When("it's NAME") {
            every { sortFilterGroupOptions.sortOrder } returns DevicesSortOrder.NAME
            then("return NAME analytics proper value") {
                sortFilterGroupOptions.getSortAnalyticsValue() shouldBe "name"
            }
        }
        When("it's LAST_ACTIVE") {
            every { sortFilterGroupOptions.sortOrder } returns DevicesSortOrder.LAST_ACTIVE
            then("return LAST_ACTIVE analytics proper value") {
                sortFilterGroupOptions.getSortAnalyticsValue() shouldBe "last_active"
            }
        }
    }

    context("Get device's filter analytics value") {
        When("it's ALL") {
            every { sortFilterGroupOptions.filterType } returns DevicesFilterType.ALL
            then("return ALL analytics proper value") {
                sortFilterGroupOptions.getFilterAnalyticsValue() shouldBe "all"
            }
        }
        When("it's OWNED") {
            every { sortFilterGroupOptions.filterType } returns DevicesFilterType.OWNED
            then("return OWNED analytics proper value") {
                sortFilterGroupOptions.getFilterAnalyticsValue() shouldBe "owned"
            }
        }
        When("it's FAVORITES ") {
            every { sortFilterGroupOptions.filterType } returns DevicesFilterType.FAVORITES
            then("return FAVORITES analytics proper value") {
                sortFilterGroupOptions.getFilterAnalyticsValue() shouldBe "favorites"
            }
        }
    }

    context("Get device's grouping analytics value") {
        When("it's NO_GROUPING ") {
            every { sortFilterGroupOptions.groupBy } returns DevicesGroupBy.NO_GROUPING
            then("return NO_GROUPING analytics proper value") {
                sortFilterGroupOptions.getGroupByAnalyticsValue() shouldBe "no_grouping"
            }
        }
        When("it's RELATIONSHIP") {
            every { sortFilterGroupOptions.groupBy } returns DevicesGroupBy.RELATIONSHIP
            then("return RELATIONSHIP analytics proper value") {
                sortFilterGroupOptions.getGroupByAnalyticsValue() shouldBe "relationship"
            }
        }
        When("it's STATUS ") {
            every { sortFilterGroupOptions.groupBy } returns DevicesGroupBy.STATUS
            then("return STATUS analytics proper value") {
                sortFilterGroupOptions.getGroupByAnalyticsValue() shouldBe "status"
            }
        }
    }
})
