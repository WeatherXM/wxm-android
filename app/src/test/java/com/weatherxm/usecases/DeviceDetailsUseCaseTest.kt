package com.weatherxm.usecases

import com.weatherxm.TestConfig.failure
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isError
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.data.models.Attributes
import com.weatherxm.data.models.BatteryState
import com.weatherxm.data.models.Device
import com.weatherxm.data.models.PublicDevice
import com.weatherxm.data.models.Relation
import com.weatherxm.data.models.Rewards
import com.weatherxm.data.models.SeverityLevel
import com.weatherxm.data.repository.DeviceOTARepository
import com.weatherxm.data.repository.DeviceRepository
import com.weatherxm.data.repository.ExplorerRepository
import com.weatherxm.data.repository.RewardsRepository
import com.weatherxm.ui.common.DeviceAlert
import com.weatherxm.ui.common.DeviceAlertType
import com.weatherxm.ui.common.DeviceRelation
import com.weatherxm.ui.common.empty
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.mockk

class DeviceDetailsUseCaseTest : BehaviorSpec({
    val deviceRepo = mockk<DeviceRepository>()
    val deviceOTARepo = mockk<DeviceOTARepository>()
    val rewardsRepo = mockk<RewardsRepository>()
    val explorerRepo = mockk<ExplorerRepository>()
    val usecase = DeviceDetailsUseCaseImpl(
        deviceRepo,
        rewardsRepo,
        explorerRepo,
        deviceOTARepo
    )

    val publicDevice = PublicDevice(
        "publicDevice",
        String.empty(),
        null,
        null,
        false,
        null,
        "cellIndex",
        null,
        null,
        null
    )
    val ownedDevice = Device(
        "ownedDevice",
        String.empty(),
        null,
        null,
        null,
        null,
        Attributes(isActive = false, null, null, null, null, null, null),
        null,
        null,
        null,
        Relation.owned,
        BatteryState.low
    )
    val uiPublicDevice = publicDevice.toUIDevice().apply {
        relation = DeviceRelation.UNFOLLOWED
        alerts = listOf(DeviceAlert(DeviceAlertType.OFFLINE, severity = SeverityLevel.ERROR))
    }
    val uiOwnedDevice = ownedDevice.toUIDevice().apply {
        alerts = listOf(
            DeviceAlert(DeviceAlertType.OFFLINE, SeverityLevel.ERROR),
            DeviceAlert(DeviceAlertType.LOW_BATTERY, SeverityLevel.WARNING)
        )
    }
    val rewards = mockk<Rewards>()

    beforeSpec {
        every { deviceOTARepo.shouldNotifyOTA(any(), any()) } returns false
    }

    context("Get device") {
        given("The repository providing the device") {
            When("it's a public device") {
                When("it's a failure") {
                    coMockEitherLeft({
                        explorerRepo.getCellDevice(uiPublicDevice.cellIndex, uiPublicDevice.id)
                    }, failure)
                    then("return the failure") {
                        usecase.getDevice(uiPublicDevice).isError()
                    }
                }
                When("it's a success") {
                    coMockEitherRight({
                        explorerRepo.getCellDevice(uiPublicDevice.cellIndex, uiPublicDevice.id)
                    }, publicDevice)
                    then("return the device") {
                        usecase.getDevice(uiPublicDevice).isSuccess(uiPublicDevice)
                    }
                }
            }
            When("it's an owned device") {
                When("it's a failure") {
                    coMockEitherLeft({ deviceRepo.getUserDevice(uiOwnedDevice.id) }, failure)
                    then("return the failure") {
                        usecase.getDevice(uiOwnedDevice).isError()
                    }
                }
                When("it's a success") {
                    coMockEitherRight({ deviceRepo.getUserDevice(uiOwnedDevice.id) }, ownedDevice)
                    then("return the device") {
                        usecase.getDevice(uiOwnedDevice).isSuccess(uiOwnedDevice)
                    }
                }
            }
        }
    }

    context("Get device rewards") {
        given("The repository providing the rewards") {
            When("it's a failure") {
                coMockEitherLeft({ rewardsRepo.getRewards(ownedDevice.id) }, failure)
                then("return the failure") {
                    usecase.getRewards(ownedDevice.id).isError()
                }
            }
            When("it's a success") {
                coMockEitherRight({ rewardsRepo.getRewards(ownedDevice.id) }, rewards)
                then("return the rewards") {
                    usecase.getRewards(ownedDevice.id).isSuccess(rewards)
                }
            }
        }
    }
})
