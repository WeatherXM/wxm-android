package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.UIForecast
import com.weatherxm.ui.common.UIRewards
import com.weatherxm.ui.explorer.UICell

interface DeviceDetailsUseCase {
    suspend fun getUserDevice(device: UIDevice): Either<Failure, UIDevice>
    suspend fun getForecast(
        device: UIDevice,
        forceRefresh: Boolean = false
    ): Either<Failure, List<UIForecast>>

    suspend fun getAddressOfCell(cell: UICell): String?
    suspend fun getRewards(deviceId: String): Either<Failure, UIRewards>
    fun isMainnetEnabled(): Boolean
    fun getMainnetMessage(): String
}
