package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.Rewards
import com.weatherxm.data.repository.AddressRepository
import com.weatherxm.data.repository.DeviceOTARepository
import com.weatherxm.data.repository.DeviceRepository
import com.weatherxm.data.repository.ExplorerRepository
import com.weatherxm.data.repository.RewardsRepository
import com.weatherxm.ui.common.DeviceAlert
import com.weatherxm.ui.common.DeviceAlertType
import com.weatherxm.ui.common.DeviceRelation
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.explorer.UICell

@Suppress("LongParameterList")
class DeviceDetailsUseCaseImpl(
    private val deviceRepository: DeviceRepository,
    private val rewardsRepository: RewardsRepository,
    private val addressRepository: AddressRepository,
    private val explorerRepository: ExplorerRepository,
    private val deviceOTARepo: DeviceOTARepository
) : DeviceDetailsUseCase {

    override suspend fun getDevice(device: UIDevice): Either<Failure, UIDevice> {
        return if (device.isUnfollowed()) {
            explorerRepository.getCellDevice(device.cellIndex, device.id).map {
                it.toUIDevice().apply {
                    this.relation = DeviceRelation.UNFOLLOWED
                    if (isActive == false) {
                        this.alerts = listOf(DeviceAlert.createError(DeviceAlertType.OFFLINE))
                    }
                }
            }
        } else {
            getUserDevice(device.id)
        }
    }

    override suspend fun getUserDevice(deviceId: String): Either<Failure, UIDevice> {
        return deviceRepository.getUserDevice(deviceId).map {
            it.toUIDevice().apply {
                createDeviceAlerts(deviceOTARepo.userShouldNotifiedOfOTA(id, assignedFirmware))
            }
        }
    }

    override suspend fun getAddressOfCell(cell: UICell): String? {
        return addressRepository.getAddressFromLocation(cell.index, cell.center)
    }

    override suspend fun getRewards(deviceId: String): Either<Failure, Rewards> {
        return rewardsRepository.getRewards(deviceId)
    }
}
