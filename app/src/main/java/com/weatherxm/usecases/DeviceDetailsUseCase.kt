package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.Rewards
import com.weatherxm.ui.common.UIDevice

interface DeviceDetailsUseCase {
    suspend fun getDevice(device: UIDevice): Either<Failure, UIDevice>
    suspend fun getRewards(deviceId: String): Either<Failure, Rewards>
    suspend fun getUserDevice(deviceId: String): Either<Failure, UIDevice>
    fun setAcceptTerms()
    fun shouldShowTermsPrompt(): Boolean
    fun showDeviceNotificationsPrompt(): Boolean
    fun checkDeviceNotificationsPrompt()
}
