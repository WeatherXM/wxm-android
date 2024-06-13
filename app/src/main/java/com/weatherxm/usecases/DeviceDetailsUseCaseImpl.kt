package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.Rewards
import com.weatherxm.data.repository.AddressRepository
import com.weatherxm.data.repository.AppConfigRepository
import com.weatherxm.data.repository.DeviceOTARepository
import com.weatherxm.data.repository.DeviceRepository
import com.weatherxm.data.repository.ExplorerRepository
import com.weatherxm.data.repository.RewardsRepository
import com.weatherxm.ui.common.DeviceAlert
import com.weatherxm.ui.common.DeviceAlertType
import com.weatherxm.ui.common.DeviceRelation
import com.weatherxm.ui.common.MainnetInfo
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.explorer.UICell

@Suppress("LongParameterList")
class DeviceDetailsUseCaseImpl(
    private val deviceRepository: DeviceRepository,
    private val rewardsRepository: RewardsRepository,
    private val addressRepository: AddressRepository,
    private val explorerRepository: ExplorerRepository,
    private val deviceOTARepository: DeviceOTARepository,
    private val appConfigRepository: AppConfigRepository
) : DeviceDetailsUseCase {

    override suspend fun getUserDevice(device: UIDevice): Either<Failure, UIDevice> {
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
            deviceRepository.getUserDevice(device.id).map {
                it.toUIDevice().apply {
                    val shouldShowOTAPrompt = deviceOTARepository.shouldShowOTAPrompt(
                        id, assignedFirmware
                    ) && isOwned()
                    val alerts = mutableListOf<DeviceAlert>()
                    if (isActive == false) {
                        alerts.add(DeviceAlert.createError(DeviceAlertType.OFFLINE))
                    }

                    if (device.hasLowBattery() && device.isOwned()) {
                        alerts.add(DeviceAlert.createWarning(DeviceAlertType.LOW_BATTERY))
                    }

                    if (shouldShowOTAPrompt && isHelium() && needsUpdate()) {
                        alerts.add(DeviceAlert.createWarning(DeviceAlertType.NEEDS_UPDATE))
                    }
                    this.alerts = alerts.sortedByDescending { alert ->
                        alert.severity
                    }
                }
            }
        }
    }

    override suspend fun getAddressOfCell(cell: UICell): String? {
        return addressRepository.getAddressFromLocation(cell.index, cell.center)
    }

    override suspend fun getRewards(deviceId: String): Either<Failure, Rewards> {
        return rewardsRepository.getRewards(deviceId)
    }

    override fun isMainnetEnabled(): Boolean {
        return appConfigRepository.isMainnetEnabled()
    }

    override fun getMainnetInfo(): MainnetInfo {
        return MainnetInfo(
            appConfigRepository.getMainnetMessage(),
            appConfigRepository.getMainnetUrl()
        )
    }
}
