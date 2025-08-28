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
    fun markOnboardingStepAsCompleted(userId: String, stepId: String)
    fun markOnboardingStepAsSkipped(userId: String, stepId: String)
    fun removeOnboardingStepFromCompleted(userId: String, stepId: String)
    fun removeOnboardingStepFromSkipped(userId: String, stepId: String)
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
    
    override fun markOnboardingStepAsCompleted(userId: String, stepId: String) {
        repository.markOnboardingStepAsCompleted(userId, stepId)
    }

    override fun markOnboardingStepAsSkipped(userId: String, stepId: String) {
        repository.markOnboardingStepAsSkipped(userId, stepId)
    }

    override fun removeOnboardingStepFromCompleted(userId: String, stepId: String) {
        repository.removeOnboardingStepFromCompleted(userId, stepId)
    }

    override fun removeOnboardingStepFromSkipped(userId: String, stepId: String) {
        repository.removeOnboardingStepFromSkipped(userId, stepId)
    }
}
