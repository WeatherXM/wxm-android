package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.ui.common.UIRewardObject

interface RewardDetailsUseCase {
    suspend fun getReward(deviceId: String, txHash: String): Either<Failure, UIRewardObject>
}
