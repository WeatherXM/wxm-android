package com.weatherxm.usecases

import com.weatherxm.TestConfig.failure
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isError
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.data.models.Attributes
import com.weatherxm.data.models.BatteryState
import com.weatherxm.data.models.Device
import com.weatherxm.data.models.Relation
import com.weatherxm.data.repository.DeviceOTARepository
import com.weatherxm.data.repository.DeviceRepository
import com.weatherxm.data.repository.UserPreferencesRepository
import com.weatherxm.ui.common.DeviceRelation
import com.weatherxm.ui.common.DevicesFilterType
import com.weatherxm.ui.common.DevicesGroupBy
import com.weatherxm.ui.common.DevicesSortFilterOptions
import com.weatherxm.ui.common.DevicesSortOrder
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.empty
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify

class DeviceListUseCaseTest : BehaviorSpec({
    val deviceRepo = mockk<DeviceRepository>()
    val deviceOTARepo = mockk<DeviceOTARepository>()
    val userPreferencesRepo = mockk<UserPreferencesRepository>()
    val usecase = DeviceListUseCaseImpl(deviceRepo, deviceOTARepo, userPreferencesRepo)

    val defaultOptions = DevicesSortFilterOptions()
    val userOptions = DevicesSortFilterOptions(
        sortOrder = DevicesSortOrder.DATE_ADDED,
        filterType = DevicesFilterType.FAVORITES,
        groupBy = DevicesGroupBy.NO_GROUPING
    )
    val devices = listOf(
        Device(
            String.empty(),
            String.empty(),
            null,
            null,
            null,
            null,
            Attributes(isActive = true, null, null, null, null, null, null),
            null,
            null,
            null,
            Relation.owned,
            null
        ),
        Device(
            String.empty(),
            String.empty(),
            null,
            null,
            null,
            null,
            Attributes(isActive = false, null, null, null, null, null, null),
            null,
            null,
            null,
            Relation.followed,
            null
        ),
        Device(
            String.empty(),
            String.empty(),
            null,
            null,
            null,
            null,
            Attributes(isActive = true, null, null, null, null, null, null),
            null,
            null,
            null,
            Relation.owned,
            BatteryState.low
        )
    )
    val uiDevices = devices.map {
        it.toUIDevice().apply {
            createDeviceAlerts(false)
        }
    }

    beforeSpec {
        justRun {
            userPreferencesRepo.setDevicesSortFilterOptions(
                defaultOptions.sortOrder.name,
                defaultOptions.filterType.name,
                defaultOptions.groupBy.name
            )
        }
        every { deviceOTARepo.shouldNotifyOTA(any(), any()) } returns false
    }

    fun mockDevicesSortFilterOptions(
        sortOrder: DevicesSortOrder,
        filterType: DevicesFilterType,
        groupBy: DevicesGroupBy
    ) {
        every {
            userPreferencesRepo.getDevicesSortFilterOptions()
        } returns listOf(
            sortOrder.name,
            filterType.name,
            groupBy.name
        )
    }

    context("Get devices") {
        given("The repository providing the devices") {
            When("it's a failure") {
                coMockEitherLeft({ deviceRepo.getUserDevices() }, failure)
                then("return the failure") {
                    usecase.getUserDevices().isError()
                }
            }
            When("it's a success") {
                coMockEitherRight({ deviceRepo.getUserDevices() }, devices)
                and("Default Sorting-Filtering-Grouping options are used") {
                    every { userPreferencesRepo.getDevicesSortFilterOptions() } returns emptyList()
                    then("return these devices with alerts and default options") {
                        usecase.getUserDevices().isSuccess(uiDevices)
                    }
                }
                and("sorting = NAME, filtering = OWNED, grouping = RELATIONSHIP") {
                    mockDevicesSortFilterOptions(
                        DevicesSortOrder.NAME, DevicesFilterType.OWNED, DevicesGroupBy.RELATIONSHIP
                    )
                    then("return these devices with alerts and the above options") {
                        val finalDevices = mutableListOf<UIDevice>()
                        uiDevices
                            .sortedBy { it.getDefaultOrFriendlyName() }
                            .filter { it.isOwned() }
                            .groupBy { it.relation }
                            .forEach {
                                if (it.key == DeviceRelation.OWNED) {
                                    finalDevices.addAll(0, it.value)
                                } else {
                                    finalDevices.addAll(it.value)
                                }
                            }
                        usecase.getUserDevices().isSuccess(finalDevices)
                    }
                }
                and("sorting = LAST_ACTIVE, filtering = FAVORITES, grouping = STATUS") {
                    mockDevicesSortFilterOptions(
                        DevicesSortOrder.LAST_ACTIVE,
                        DevicesFilterType.FAVORITES,
                        DevicesGroupBy.STATUS
                    )
                    then("return these devices with alerts and the above options") {
                        val finalDevices = mutableListOf<UIDevice>()
                        uiDevices
                            .sortedByDescending { it.lastWeatherStationActivity }
                            .filter { it.isFollowed() }
                            .groupBy { it.isActive }
                            .forEach {
                                finalDevices.addAll(it.value)
                            }
                        usecase.getUserDevices().isSuccess(finalDevices)
                    }
                }
            }
        }
    }

    context("Get devices sort filter options") {
        given("The repository providing these options") {
            When("the saved data is empty (user is using default)") {
                every { userPreferencesRepo.getDevicesSortFilterOptions() } returns emptyList()
                then("return the default") {
                    usecase.getDevicesSortFilterOptions() shouldBe defaultOptions
                }
            }
            When("there are actual saved options") {
                mockDevicesSortFilterOptions(
                    DevicesSortOrder.DATE_ADDED,
                    DevicesFilterType.FAVORITES,
                    DevicesGroupBy.NO_GROUPING
                )
                usecase.getDevicesSortFilterOptions() shouldBe userOptions
            }
        }
    }

    context("Set devices sort filter options") {
        given("the repository providing that SET mechanism") {
            then("call the repository respective function") {
                usecase.setDevicesSortFilterOptions(defaultOptions)
                verify(exactly = 1) {
                    userPreferencesRepo.setDevicesSortFilterOptions(
                        defaultOptions.sortOrder.name,
                        defaultOptions.filterType.name,
                        defaultOptions.groupBy.name
                    )
                }
            }
        }
    }
})
