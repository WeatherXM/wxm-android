package com.weatherxm.usecases

import android.content.Context
import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.repository.AppConfigRepository
import com.weatherxm.data.repository.RewardsRepository
import com.weatherxm.ui.common.UIRewardObject

class RewardDetailsUseCaseImpl(
    private val rewardsRepository: RewardsRepository,
    private val appConfigRepository: AppConfigRepository,
    private val context: Context
) : RewardDetailsUseCase {

    override suspend fun getReward(
        deviceId: String,
        txHash: String
    ): Either<Failure, UIRewardObject> {
        return rewardsRepository.getRewardDetails(deviceId, txHash).map {
            UIRewardObject(context, it, appConfigRepository.getRewardsHideAnnotationThreshold())
        }
    }
}
