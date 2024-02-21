package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.repository.AppConfigRepository
import com.weatherxm.data.repository.RewardsRepository
import com.weatherxm.ui.common.UIRewardsTimeline

interface RewardsUseCase {
    suspend fun getRewardsTimeline(
        deviceId: String,
        page: Int?,
        fromDate: String?,
        toDate: String? = null
    ): Either<Failure, UIRewardsTimeline>

    fun getRewardsHideAnnotationThreshold(): Long
}

class RewardsUseCaseImpl(
    private val repository: RewardsRepository,
    private val appConfigRepository: AppConfigRepository
) : RewardsUseCase {
    override suspend fun getRewardsTimeline(
        deviceId: String,
        page: Int?,
        fromDate: String?,
        toDate: String?
    ): Either<Failure, UIRewardsTimeline> {
        return repository.getRewardsTimeline(
            deviceId = deviceId,
            page = page,
            fromDate = fromDate,
            toDate = toDate
        ).map {
            if (it.data.isEmpty()) {
                UIRewardsTimeline(listOf(), reachedTotal = true)
            } else {
                UIRewardsTimeline(
                    it.data.filter { tx ->
                        // Keep only transactions that have a reward for this device
                        tx.baseReward != null
                    },
                    it.hasNextPage
                )
            }
        }
    }

    override fun getRewardsHideAnnotationThreshold(): Long {
        return appConfigRepository.getRewardsHideAnnotationThreshold()
    }
}
