package com.weatherxm.ui.common

import com.weatherxm.data.models.RewardSplit
import com.weatherxm.data.repository.RewardsRepositoryImpl
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.core.spec.style.scopes.BehaviorSpecWhenContainerScope
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlin.random.Random
import kotlin.random.nextInt

class UIModelsTest : BehaviorSpec({
    val device = mockk<UIDevice>()
    val sortFilterGroupOptions = mockk<DevicesSortFilterOptions>()
    val rewardSplitsData = mockk<RewardSplitsData>()
    val deviceTotalRewardDetails = mockk<DeviceTotalRewardsDetails>()

    beforeSpec {
        every { device.isOwned() } answers { callOriginal() }
        every { device.isFollowed() } answers { callOriginal() }
        every { device.isUnfollowed() } answers { callOriginal() }
        every { device.shouldPromptUpdate() } answers { callOriginal() }
        every { device.isEmpty() } answers { callOriginal() }
        every { device.isOnline() } answers { callOriginal() }
        every { device.isHelium() } answers { callOriginal() }
        every { device.isWifi() } answers { callOriginal() }
        every { device.isCellular() } answers { callOriginal() }
        every { device.alerts } answers { callOriginal() }
        every { device.createDeviceAlerts(any()) } answers { callOriginal() }
        every { device.hasErrors() } answers { callOriginal() }
        every { device.getLastCharsOfLabel() } answers { callOriginal() }

        every { sortFilterGroupOptions.getSortAnalyticsValue() } answers { callOriginal() }
        every { sortFilterGroupOptions.getFilterAnalyticsValue() } answers { callOriginal() }
        every { sortFilterGroupOptions.getGroupByAnalyticsValue() } answers { callOriginal() }

        every { rewardSplitsData.hasSplitRewards() } answers { callOriginal() }

        every { deviceTotalRewardDetails.isEmpty() } answers { callOriginal() }
    }

    suspend fun BehaviorSpecWhenContainerScope.testDeviceRelation(
        isOwned: Boolean,
        isFollowed: Boolean,
        isUnfollowed: Boolean
    ) {
        then("function isOwned should return $isOwned") {
            device.isOwned() shouldBe isOwned
        }
        then("function isFollowed should return $isFollowed") {
            device.isFollowed() shouldBe isFollowed
        }
        then("function isUnfollowed should return $isUnfollowed") {
            device.isUnfollowed() shouldBe isUnfollowed
        }
    }

    suspend fun BehaviorSpecWhenContainerScope.testConnectivity(
        isHelium: Boolean,
        isWifi: Boolean,
        isCellular: Boolean
    ) {
        then("function isHelium should return $isHelium") {
            device.isHelium() shouldBe isHelium
        }
        then("function isWifi should return $isWifi") {
            device.isWifi() shouldBe isWifi
        }
        then("function isCellular should return $isCellular") {
            device.isCellular() shouldBe isCellular
        }
    }

    context("Get device's relation") {
        When("it's null") {
            every { device.relation } returns null
            testDeviceRelation(isOwned = false, isFollowed = false, isUnfollowed = false)
        }
        When("it's owned") {
            every { device.relation } returns DeviceRelation.OWNED
            testDeviceRelation(isOwned = true, isFollowed = false, isUnfollowed = false)
        }
        When("it's followed") {
            every { device.relation } returns DeviceRelation.FOLLOWED
            testDeviceRelation(isOwned = false, isFollowed = true, isUnfollowed = false)
        }
        When("it's unfollowed") {
            every { device.relation } returns DeviceRelation.UNFOLLOWED
            testDeviceRelation(isOwned = false, isFollowed = false, isUnfollowed = true)
        }
    }

    context("Get if the device should be prompted to be updated") {
        When("the device is not owned") {
            every { device.relation } returns DeviceRelation.UNFOLLOWED
            then("return false") {
                device.shouldPromptUpdate() shouldBe false
            }
        }
        When("the device is owned") {
            every { device.relation } returns DeviceRelation.OWNED
            and("current firmware == assigned firmware") {
                every { device.currentFirmware } returns "1.0.0"
                every { device.assignedFirmware } returns "1.0.0"
                then("return false") {
                    device.shouldPromptUpdate() shouldBe false
                }
            }
            and("current firmware != assigned firmware") {
                and("assigned firmware is null") {
                    every { device.assignedFirmware } returns null
                    then("return false") {
                        device.shouldPromptUpdate() shouldBe false
                    }
                }
                and("assigned firmware is empty") {
                    every { device.assignedFirmware } returns ""
                    then("return false") {
                        device.shouldPromptUpdate() shouldBe false
                    }
                }
                and("assigned firmware is nor null or empty") {
                    every { device.assignedFirmware } returns "2.0.0"
                    and("it's an H1 bundle") {
                        every { device.bundleName } returns BundleName.h1
                        then("return true") {
                            device.shouldPromptUpdate() shouldBe true
                        }
                    }
                    and("it's an H2 bundle") {
                        every { device.bundleName } returns BundleName.h2
                        then("return true") {
                            device.shouldPromptUpdate() shouldBe true
                        }
                    }
                    and("it's a D1 bundle") {
                        every { device.bundleName } returns BundleName.d1
                        then("return false") {
                            device.shouldPromptUpdate() shouldBe false
                        }
                    }
                    and("it's a M5 bundle") {
                        every { device.bundleName } returns BundleName.m5
                        then("return false") {
                            device.shouldPromptUpdate() shouldBe false
                        }
                    }
                    and("it's a Pulse bundle") {
                        every { device.bundleName } returns BundleName.pulse
                        then("return false") {
                            device.shouldPromptUpdate() shouldBe false
                        }
                    }
                }
            }
        }
    }

    context("Get if the device is empty or not") {
        When("the device has a non-empty ID") {
            every { device.id } returns "deviceId"
            then("return false") {
                device.isEmpty() shouldBe false
            }
        }
        When("the device has an empty ID") {
            every { device.id } returns String.empty()
            and("the device has a name") {
                every { device.name } returns "deviceName"
                then("return false") {
                    device.isEmpty() shouldBe false
                }
            }
            and("the device has an empty name") {
                every { device.name } returns String.empty()
                and("the device has a cell index") {
                    every { device.cellIndex } returns "cellIndex"
                    then("return false") {
                        device.isEmpty() shouldBe false
                    }
                }
                and("the device has an empty cell index") {
                    every { device.cellIndex } returns String.empty()
                    then("return true") {
                        device.isEmpty() shouldBe true
                    }
                }
            }
        }
    }

    context("Get if a device is online or not") {
        When("isActive is null") {
            every { device.isActive } returns null
            then("return false") {
                device.isOnline() shouldBe false
            }
        }
        When("isActive is false") {
            every { device.isActive } returns false
            then("return false") {
                device.isOnline() shouldBe false
            }
        }
        /**
         * The below fails incorrectly, more info: https://github.com/mockk/mockk/issues/1321
         */
//        When("isActive is true") {
//            every { device.isActive } returns true
//            then("return true") {
//                device.isOnline() shouldBe true
//            }
//        }
    }

    context("Get device's connectivity type") {
        When("connectivity is null") {
            every { device.connectivity } returns null
            testConnectivity(isHelium = false, isWifi = false, isCellular = false)
        }
        When("connectivity is 'helium'") {
            every { device.connectivity } returns "helium"
            testConnectivity(isHelium = true, isWifi = false, isCellular = false)
        }
        When("connectivity is 'wifi'") {
            every { device.connectivity } returns "wifi"
            testConnectivity(isHelium = false, isWifi = true, isCellular = false)
        }
        When("connectivity is 'cellular'") {
            every { device.connectivity } returns "cellular"
            testConnectivity(isHelium = false, isWifi = false, isCellular = true)
        }
    }

    context("Create device alerts") {
        When("We have some alerts to show") {
            every { device.shouldPromptUpdate() } returns true
            /**
             * The below fails incorrectly, more info: https://github.com/mockk/mockk/issues/1321
             *
             * So until the above is fixed, we have 1 alert, only the DeviceAlertType.NEEDS_UPDATE
             */
            every { device.isActive } returns false
            every { device.hasLowBattery } returns null
            device.createDeviceAlerts(true)

            then("We should have 1 alerts") {
                device.alerts.size shouldBe 1
            }
            then("we should have the needs update alert") {
                //   device.alerts[0].alert shouldBe DeviceAlertType.OFFLINE
                device.alerts[0].alert shouldBe DeviceAlertType.NEEDS_UPDATE
            }
            then("the device should not have any errors") {
                device.hasErrors() shouldBe false
            }
        }
    }

    context("Get the last chars of the label") {
        When("the label is null") {
            every { device.label } returns null
            then("return an empty string") {
                device.getLastCharsOfLabel() shouldBe String.empty()
            }
        }
        When("the label is valid") {
            every { device.label } returns "00:00:00"
            then("return an the last 6 chars") {
                device.getLastCharsOfLabel() shouldBe "000000"
            }
        }
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

    context("Get if a RewardSplitsData has split rewards") {
        When("the list size is smaller than two") {
            val splits = mutableListOf<RewardSplit>()
            repeat(Random.nextInt(0..1)) {
                splits.add(mockk())
            }
            every { rewardSplitsData.splits } returns splits
            then("return false") {
                rewardSplitsData.hasSplitRewards() shouldBe false
            }
        }
        When("the list size is equal or higher than two") {
            val splits = mutableListOf<RewardSplit>()
            repeat(Random.nextInt(2..10)) {
                splits.add(mockk())
            }
            every { rewardSplitsData.splits } returns splits
            then("return true") {
                rewardSplitsData.hasSplitRewards() shouldBe true
            }
        }
    }

    context("Get if the deviceTotalRewardsDetails is empty or not") {
        When("the total is not null") {
            every { deviceTotalRewardDetails.total } returns 10F
            then("return false") {
                deviceTotalRewardDetails.isEmpty() shouldBe false
            }
        }
        When("the total is null") {
            every { deviceTotalRewardDetails.total } returns null
            and("the mode is not null") {
                every {
                    deviceTotalRewardDetails.mode
                } returns RewardsRepositoryImpl.Companion.RewardsSummaryMode.YEAR
                then("return false") {
                    deviceTotalRewardDetails.isEmpty() shouldBe false
                }
            }
            and("the mode is null") {
                every { deviceTotalRewardDetails.mode } returns null
                and("the totals are not empty") {
                    every { deviceTotalRewardDetails.totals } returns listOf(mockk())
                    then("return false") {
                        deviceTotalRewardDetails.isEmpty() shouldBe false
                    }
                }
                and("the totals are empty") {
                    every { deviceTotalRewardDetails.totals } returns emptyList()
                    and("the boosts are not empty") {
                        every { deviceTotalRewardDetails.boosts } returns listOf(mockk())
                        then("return false") {
                            deviceTotalRewardDetails.isEmpty() shouldBe false
                        }
                    }
                    and("the boosts are empty") {
                        every { deviceTotalRewardDetails.boosts } returns emptyList()
                        and("the status is not LOADING") {
                            every { deviceTotalRewardDetails.status } returns Status.SUCCESS
                            then("return false") {
                                deviceTotalRewardDetails.isEmpty() shouldBe false
                            }
                        }
                        and("the status is LOADING") {
                            every { deviceTotalRewardDetails.status } returns Status.LOADING
                            then("return true") {
                                deviceTotalRewardDetails.isEmpty() shouldBe true
                            }
                        }
                    }
                }
            }
        }
    }
})
