package com.weatherxm.usecases

import arrow.core.Either
import arrow.core.flatMap
import com.weatherxm.data.models.QuestUser
import com.weatherxm.data.models.QuestUserProgress
import com.weatherxm.data.repository.QuestsRepository
import com.weatherxm.ui.common.QuestOnboardingData
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import timber.log.Timber

interface QuestsUseCase {
    fun fetchUser(userId: String): Either<Throwable, QuestUser>
    suspend fun fetchOnboardingData(userId: String): Either<Throwable, QuestOnboardingData>
    suspend fun completeQuest(userId: String, questId: String): Either<Throwable, Unit>
    suspend fun markQuestStepAsCompleted(
        userId: String,
        questId: String,
        stepId: String
    ): Either<Throwable, Unit>

    suspend fun markQuestStepAsSkipped(
        userId: String,
        questId: String,
        stepId: String
    ): Either<Throwable, Unit>

    suspend fun setWallet(
        userId: String,
        chainId: String,
        walletAddress: String
    ): Either<Throwable, Unit>
}

class QuestsUseCaseImpl(val repository: QuestsRepository) : QuestsUseCase {
    override fun fetchUser(userId: String): Either<Throwable, QuestUser> {
        return repository.fetchUser(userId)
    }

    override suspend fun fetchOnboardingData(userId: String): Either<Throwable, QuestOnboardingData> {
        return coroutineScope {
            /**
             * Fetch onboarding progress and quests in parallel
             */
            val onboardingProgressDeferred = async { repository.fetchOnboardingProgress(userId) }
            val onboardingQuestDeferred = async { repository.fetchOnboardingQuest() }

            val onboardingProgressResult = onboardingProgressDeferred.await()
            val onboardingQuestResult = onboardingQuestDeferred.await()

            /**
             * Handle both results, if any fails, return an error.
             */
            var onboardingProgress: QuestUserProgress? = null
            onboardingProgressResult.onRight {
                onboardingProgress = it
            }.onLeft {
                Timber.e(it, "[Firestore]: Error when fetching user's onboarding progress")
                return@coroutineScope Either.Left(it)
            }

            return@coroutineScope onboardingQuestResult.flatMap {
                Either.Right(QuestOnboardingData(it, onboardingProgress))
            }.onLeft {
                Timber.e(it, "[Firestore]: Error when fetching the onboarding quest")
                Either.Left(it)
            }
        }
    }

    override suspend fun completeQuest(userId: String, questId: String): Either<Throwable, Unit> {
        return repository.completeQuest(userId, questId)
    }

    override suspend fun markQuestStepAsCompleted(
        userId: String,
        questId: String,
        stepId: String
    ): Either<Throwable, Unit> {
        return repository.markQuestStepAsCompleted(userId, questId, stepId)
    }

    override suspend fun markQuestStepAsSkipped(
        userId: String,
        questId: String,
        stepId: String
    ): Either<Throwable, Unit> {
        return repository.markQuestStepAsSkipped(userId, questId, stepId)
    }

    override suspend fun setWallet(
        userId: String,
        chainId: String,
        walletAddress: String
    ): Either<Throwable, Unit> {
        return repository.setWallet(userId, chainId, walletAddress)
    }
}
