package com.weatherxm.ui.home.quests.steps

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.weatherxm.ui.common.QuestStep
import com.weatherxm.usecases.QuestsUseCase
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.android.ext.android.inject
import kotlin.getValue


class GenericStepViewModel(
    val questStep: QuestStep,
    private val usecase: QuestsUseCase,
    private val dispatcher: CoroutineDispatcher
): ViewModel() {

    fun markStepAsCompleted(userId: String) {
        usecase.markOnboardingStepAsCompleted(userId, questStep.id)
    }

    fun removeStepFromCompleted(userId: String) {
        usecase.removeOnboardingStepFromCompleted(userId, questStep.id)
    }

    fun markStepAsSkipped(userId: String) {
        usecase.markOnboardingStepAsSkipped(userId, questStep.id)
    }

    fun removeStepFromSkipped(userId: String) {
        usecase.removeOnboardingStepFromSkipped(userId, questStep.id)
    }
}
