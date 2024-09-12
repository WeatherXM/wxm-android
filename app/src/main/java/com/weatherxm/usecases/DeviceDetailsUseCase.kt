package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.Rewards
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.explorer.UICell

interface DeviceDetailsUseCase {
    suspend fun getDevice(device: UIDevice): Either<Failure, UIDevice>
    suspend fun getAddressOfCell(cell: UICell): String?
    suspend fun getRewards(deviceId: String): Either<Failure, Rewards>
    suspend fun getUserDevice(deviceId: String): Either<Failure, UIDevice>
}
