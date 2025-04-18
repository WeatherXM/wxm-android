package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.Rewards
import com.weatherxm.data.repository.DeviceOTARepository
import com.weatherxm.data.repository.DeviceRepository
import com.weatherxm.data.repository.ExplorerRepository
import com.weatherxm.data.repository.RewardsRepository
import com.weatherxm.data.repository.UserPreferencesRepository
import com.weatherxm.ui.common.DeviceAlert
import com.weatherxm.ui.common.DeviceAlertType
import com.weatherxm.ui.common.DeviceRelation
import com.weatherxm.ui.common.UIDevice

@Suppress("LongParameterList")
class DeviceDetailsUseCaseImpl(
    private val deviceRepository: DeviceRepository,
    private val rewardsRepository: RewardsRepository,
    private val explorerRepository: ExplorerRepository,
    private val deviceOTARepo: DeviceOTARepository,
    private val userPreferencesRepository: UserPreferencesRepository
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
                createDeviceAlerts(deviceOTARepo.shouldNotifyOTA(id, assignedFirmware))
            }
        }
    }

    override suspend fun getRewards(deviceId: String): Either<Failure, Rewards> {
        return rewardsRepository.getRewards(deviceId)
    }

    override fun shouldShowTermsPrompt() = userPreferencesRepository.shouldShowTermsPrompt()
    override fun setAcceptTerms() = userPreferencesRepository.setAcceptTerms()
}
