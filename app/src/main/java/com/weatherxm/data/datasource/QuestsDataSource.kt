package com.weatherxm.data.datasource

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.getOrElse
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import com.weatherxm.data.models.QuestFirestore
import com.weatherxm.data.models.QuestFirestoreStep
import com.weatherxm.data.models.QuestUser
import com.weatherxm.data.models.QuestUserProgress
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
}

class QuestsDataSourceImpl : QuestsDataSource, KoinComponent {
    private val firebaseFirestore: FirebaseFirestore by inject()

    private fun userDocument(userId: String) =
        firebaseFirestore.collection("users").document(userId)

    private fun onboardingQuestDocument() =
        firebaseFirestore.collection("quests").document("onboarding")

    private fun onboardingProgressDocument(userId: String) = firebaseFirestore.collection("users")
        .document(userId)
        .collection("progress")
        .document("onboarding")

    override fun fetchOnboardingProgress(userId: String): Either<Throwable, QuestUserProgress> {
        /**
         * Fetch user's onboarding progress document
         */
        return onboardingProgressDocument(userId).get().safeAwait().flatMap { document ->
            /**
             * If progress exists, use it; else create
             */
            document.toObject<QuestUserProgress>()?.let {
                Timber.d("[Firestore] Got User's Progress on Onboarding Quest: $it")
                Either.Right(it)
            } ?: run {
                val emptyProgress = QuestUserProgress.empty()
                val setResult = onboardingProgressDocument(userId).set(emptyProgress).safeAwait()

                setResult.map {
                    Timber.d("[Firestore] Empty User's Progress on Onboarding Quest created")
                    emptyProgress
                }
            }
        }
    }

    override fun fetchUser(userId: String): Either<Throwable, QuestUser> {
        /**
         * Fetch user document
         */
        return userDocument(userId).get().safeAwait().flatMap { userDoc ->
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

    @Suppress("ReturnCount")
    override suspend fun fetchOnboardingQuest(): Either<Throwable, QuestWithStepsFirestore> {
        val onboardingResult = onboardingQuestDocument().get().safeAwait()

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
                async { fetchQuestStep(stepId) }
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

    private fun fetchQuestStep(stepId: String): Either<Throwable, QuestFirestoreStep> {
        val stepResult = onboardingQuestDocument()
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
}
