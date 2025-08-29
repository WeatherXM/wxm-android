package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.data.datasource.QuestsDataSource
import com.weatherxm.data.models.QuestUser
import com.weatherxm.data.models.QuestUserProgress
import com.weatherxm.data.models.QuestWithStepsFirestore

interface QuestsRepository {
    fun fetchOnboardingProgress(userId: String): Either<Throwable, QuestUserProgress>
    fun fetchUser(userId: String): Either<Throwable, QuestUser>
    suspend fun fetchOnboardingQuest(): Either<Throwable, QuestWithStepsFirestore>
    suspend fun completeQuest(userId: String, questId: String): Either<Throwable, Unit>
    suspend fun markQuestStepAsCompleted(userId: String,
                                         questId: String,
                                         stepId: String): Either<Throwable, Unit>
    suspend fun markQuestStepAsSkipped(userId: String,
                                       questId: String,
                                       stepId: String): Either<Throwable, Unit>
}

class QuestsRepositoryImpl(val datasource: QuestsDataSource) : QuestsRepository {
    override fun fetchOnboardingProgress(userId: String): Either<Throwable, QuestUserProgress> {
        return datasource.fetchOnboardingProgress(userId)
    }

    override fun fetchUser(userId: String): Either<Throwable, QuestUser> {
        return datasource.fetchUser(userId)
    }

    override suspend fun fetchOnboardingQuest(): Either<Throwable, QuestWithStepsFirestore> {
        return datasource.fetchOnboardingQuest()
    }

    override suspend fun completeQuest(userId: String, questId: String): Either<Throwable, Unit> {
        return datasource.completeQuest(userId, questId)
    }
    
    override suspend fun markQuestStepAsCompleted(userId: String,
                                                  questId: String,
                                                  stepId: String): Either<Throwable, Unit> {
        return datasource.markQuestStepAsCompleted(userId, questId, stepId)
    }

    override suspend fun markQuestStepAsSkipped(userId: String,
                                                questId: String,
                                                stepId: String): Either<Throwable, Unit> {
        return datasource.markQuestStepAsSkipped(userId, questId, stepId)
    }
}
