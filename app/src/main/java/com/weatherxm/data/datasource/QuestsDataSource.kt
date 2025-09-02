package com.weatherxm.data.datasource

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.getOrElse
import arrow.core.right
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import com.weatherxm.data.models.QuestFirestore
import com.weatherxm.data.models.QuestFirestoreStep
import com.weatherxm.data.models.QuestUser
import com.weatherxm.data.models.QuestUserProgress
import com.weatherxm.data.models.QuestUserWallet
import com.weatherxm.data.models.QuestWithStepsFirestore
import com.weatherxm.data.safeAwait
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

interface QuestsDataSource {
    fun fetchUser(userId: String): Either<Throwable, QuestUser>
    fun fetchOnboardingProgress(userId: String): Either<Throwable, QuestUserProgress>
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

@Suppress("TooManyFunctions")
class QuestsDataSourceImpl : QuestsDataSource, KoinComponent {
    companion object {
        const val ONBOARDING_ID = "onboarding"
        const val SOLANA_CHAIN_ID = "SOL"
    }

    private val firebaseFirestore: FirebaseFirestore by inject()

    private fun userDocument(userId: String) =
        firebaseFirestore.collection("users").document(userId)

    private fun questDocument(questId: String) =
        firebaseFirestore.collection("quests").document(questId)

    private fun questProgressDocument(userId: String, questId: String) = userDocument(userId)
        .collection("progress")
        .document(questId)

    override fun fetchOnboardingProgress(userId: String): Either<Throwable, QuestUserProgress> {
        /**
         * Fetch user's onboarding progress document
         */
        return questProgressDocument(userId, ONBOARDING_ID).get().safeAwait().flatMap { document ->
            /**
             * If progress exists, use it; else create
             */
            document.toObject<QuestUserProgress>()?.let {
                Timber.d("[Firestore] Got User's Progress on Onboarding Quest: $it")
                Either.Right(it)
            } ?: run {
                val emptyProgress = QuestUserProgress.empty()
                val setResult =
                    questProgressDocument(userId, ONBOARDING_ID).set(emptyProgress).safeAwait()

                setResult.map {
                    Timber.d("[Firestore] Empty User's Progress on Onboarding Quest created")
                    emptyProgress
                }
            }
        }
    }

    override fun fetchUser(userId: String): Either<Throwable, QuestUser> {
        /**
         * Refresh earned tokens if needed and then fetch user document
         */
        return refreshEarnedTokens(userId)
            .onLeft {
                Timber.e(it, "[Firestore] Error refreshing earned tokens for user $userId")
            }
            .flatMap {
                userDocument(userId).get().safeAwait().flatMap { userDoc ->
                    /**
                     * If user exists, use it; else create
                     */
                    userDoc.toObject<QuestUser>()?.let {
                        Timber.d("[Firestore] Got Quest User: $it")
                        Either.Right(it)
                    } ?: run {
                        val newQuestUser = QuestUser(userId, 0, 0)
                        userDocument(userId).set(newQuestUser).safeAwait().map {
                            Timber.d("[Firestore] New user created: $newQuestUser")
                            newQuestUser
                        }
                    }
                }
            }
    }

    @Suppress("ReturnCount")
    override suspend fun fetchOnboardingQuest(): Either<Throwable, QuestWithStepsFirestore> {
        val onboardingResult = questDocument(ONBOARDING_ID).get().safeAwait()

        /**
         * Get the value otherwise terminate and return the Throwable.
         */
        val questData = onboardingResult.getOrElse {
            return Either.Left(it)
        }?.toObject<QuestFirestore>()

        if (questData == null) {
            return Either.Left(Throwable("No onboarding quest found"))
        }

        /**
         * Fetch all steps of the quest in parallel.
         */
        val stepIds = listOf("step1", "step2", "step3", "step4", "step5")
        val deferredSteps = stepIds.map { stepId ->
            coroutineScope {
                async { fetchQuestStep(stepId, ONBOARDING_ID) }
            }
        }

        /**
         * Gather results, check for errors and combine the results.
         */
        val stepsResults = deferredSteps.awaitAll()
        val steps = mutableListOf<QuestFirestoreStep>()
        stepsResults.forEach { result ->
            when (result) {
                is Either.Right -> steps.add(result.value)
                is Either.Left -> return Either.Left(result.value)
            }
        }
        val onboardingQuestWithSteps = QuestWithStepsFirestore(
            questData = questData,
            steps = steps.toList()
        )
        Timber.d("[Firestore] Got Quest Data With Steps: $onboardingQuestWithSteps")

        return Either.Right(onboardingQuestWithSteps)
    }

    override suspend fun completeQuest(userId: String, questId: String): Either<Throwable, Unit> {
        return questProgressDocument(userId, questId).update("isCompleted", true)
            .safeAwait()
            .map {}
    }

    private fun fetchQuestStep(
        stepId: String,
        questId: String
    ): Either<Throwable, QuestFirestoreStep> {
        val stepResult = questDocument(questId)
            .collection("steps")
            .document(stepId)
            .get()
            .safeAwait()

        return stepResult.flatMap { document ->
            document.toObject<QuestFirestoreStep>()?.let {
                it.stepId = stepId
                Either.Right(it)
            } ?: Either.Left(Throwable("No step found: $stepId"))
        }
    }

    override suspend fun markQuestStepAsCompleted(
        userId: String,
        questId: String,
        stepId: String
    ): Either<Throwable, Unit> {
        return questProgressDocument(userId, questId)
            .update("skippedSteps", FieldValue.arrayRemove(stepId))
            .safeAwait()
            .onLeft {
                Timber.e(
                    it,
                    "[Firestore] Error removing $stepId from skippedSteps " +
                        "for quest $questId, user $userId."
                )
            }
            .flatMap { // If the above was successful (Either.Right), proceed to mark as completed
                questProgressDocument(userId, questId)
                    .update("completedSteps", FieldValue.arrayUnion(stepId))
                    .safeAwait()
                    .map { }
            }
    }

    override suspend fun markQuestStepAsSkipped(
        userId: String,
        questId: String,
        stepId: String
    ): Either<Throwable, Unit> {
        return questProgressDocument(userId, questId)
            .update("completedSteps", FieldValue.arrayRemove(stepId))
            .safeAwait()
            .onLeft {
                Timber.e(
                    it,
                    "[Firestore] Error removing $stepId from completedSteps " +
                        "for quest $questId, user $userId."
                )
            }
            .flatMap {
                questProgressDocument(userId, questId)
                    .update(
                        "skippedSteps",
                        FieldValue.arrayUnion(stepId)
                    )
                    .safeAwait()
                    .map { }
            }
    }

    override suspend fun setWallet(
        userId: String,
        chainId: String,
        walletAddress: String
    ): Either<Throwable, Unit> {
        return userDocument(userId)
            .collection("wallet")
            .document(chainId)
            .set(QuestUserWallet(walletAddress))
            .safeAwait()
            .onLeft {
                Timber.e(it, "[Firestore] Error setting wallet for chain $chainId, user $userId.")
            }
            .map {}
    }
    
    private fun updateEarnedTokens(userId: String, tokens: Int): Either<Throwable, Unit> {
        return userDocument(userId)
            .update("earnedTokens", tokens)
            .safeAwait()
            .map { }
    }

    private fun listUserProgressQuestIds(userId: String): Either<Throwable, List<String>> {
        val collectionRef = userDocument(userId).collection("progress")
        return collectionRef.get().safeAwait().map { querySnapshot ->
            querySnapshot.documents.mapNotNull { it.id }
        }
    }

    @Suppress("ReturnCount")
    private fun sumAllEarnedTokens(userId: String): Either<Throwable, Int> {
        return listUserProgressQuestIds(userId).flatMap { questIds ->
            var earnedTokens = 0
            for (questId in questIds) {
                // Fetch quest document. If it fails, return the error.
                val questDocResult = questDocument(questId).get().safeAwait()
                questDocResult.getOrElse { return Either.Left(it) }

                // Fetch quest progress. If it fails, return the error.
                val questProgressResult = questProgressDocument(userId, questId)
                    .get()
                    .safeAwait()
                val questProgress = questProgressResult.getOrElse { return Either.Left(it) }
                    ?.toObject<QuestUserProgress>()

                // If questProgress is null (should not happen if previous steps succeeded),
                // consider it an error.
                if (questProgress == null) {
                    return Either.Left(Throwable("Quest progress not found for $questId"))
                }

                questProgress.completedSteps?.forEach { stepId ->
                    // Fetch quest step. If it fails, return the error.
                    val stepResult = fetchQuestStep(stepId, questId)
                    val step = stepResult.getOrElse { return Either.Left(it) }
                    earnedTokens += step.tokens ?: 0
                }
            }
            earnedTokens.right()
        }
    }

    private fun refreshEarnedTokens(userId: String): Either<Throwable, Unit> {
        return sumAllEarnedTokens(userId).flatMap {
            updateEarnedTokens(userId, it)
        }
    }
}
