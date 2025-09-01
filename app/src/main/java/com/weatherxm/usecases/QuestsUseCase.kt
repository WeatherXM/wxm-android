package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.models.QuestUser
import com.weatherxm.data.models.QuestUserProgress
import com.weatherxm.data.models.QuestWithStepsFirestore
import com.weatherxm.data.repository.QuestsRepository

interface QuestsUseCase {
    fun fetchOnboardingProgress(userId: String): Either<Throwable, QuestUserProgress>
    fun fetchUser(userId: String): Either<Throwable, QuestUser>
    suspend fun fetchOnboardingQuest(): Either<Throwable, QuestWithStepsFirestore>
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
    override fun fetchOnboardingProgress(userId: String): Either<Throwable, QuestUserProgress> {
        return repository.fetchOnboardingProgress(userId)
    }

    override fun fetchUser(userId: String): Either<Throwable, QuestUser> {
        return repository.fetchUser(userId)
    }

    override suspend fun fetchOnboardingQuest(): Either<Throwable, QuestWithStepsFirestore> {
        return repository.fetchOnboardingQuest()
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
