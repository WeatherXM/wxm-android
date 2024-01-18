package com.weatherxm.usecases

import android.content.Context
import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.repository.AppConfigRepository
import com.weatherxm.data.repository.RewardsRepository
import com.weatherxm.ui.common.UIRewardObject
import com.weatherxm.ui.common.UIRewardsList

interface RewardsUseCase {
    suspend fun getTransactions(
        deviceId: String,
        page: Int?,
        fromDate: String?,
        toDate: String? = null
    ): Either<Failure, UIRewardsList>
}

class RewardsUseCaseImpl(
    private val repository: RewardsRepository,
    private val appConfigRepo: AppConfigRepository,
    private val context: Context
) : RewardsUseCase {
    override suspend fun getTransactions(
        deviceId: String,
        page: Int?,
        fromDate: String?,
        toDate: String?
    ): Either<Failure, UIRewardsList> {
        return repository.getTransactions(
            deviceId = deviceId,
            page = page,
            fromDate = fromDate,
            toDate = toDate
        ).map {
            if (it.data.isEmpty()) {
                UIRewardsList(listOf(), reachedTotal = true)
            } else {
                val uiTransactions = it.data
                    .filter { tx ->
                        // Keep only transactions that have a reward for this device
                        tx.actualReward != null
                    }
                    .map { tx ->
                        UIRewardObject(
                            context, tx, appConfigRepo.getRewardsHideAnnotationThreshold()
                        )
                    }
                UIRewardsList(uiTransactions, it.hasNextPage)
            }
        }
    }
}
