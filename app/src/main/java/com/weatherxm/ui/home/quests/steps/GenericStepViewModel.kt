package com.weatherxm.ui.home.quests.steps

import androidx.lifecycle.ViewModel
import com.weatherxm.ui.common.QuestStep
import com.weatherxm.usecases.QuestsUseCase

class GenericStepViewModel(
    val questStep: QuestStep,
    private val useCase: QuestsUseCase
): ViewModel() {

    fun markStepAsCompleted(userId: String) {
        useCase.markOnboardingStepAsCompleted(userId, questStep.id)
    }

    fun removeStepFromCompleted(userId: String) {
        useCase.removeOnboardingStepFromCompleted(userId, questStep.id)
    }

    fun markStepAsSkipped(userId: String) {
        useCase.markOnboardingStepAsSkipped(userId, questStep.id)
    }

    fun removeStepFromSkipped(userId: String) {
        useCase.removeOnboardingStepFromSkipped(userId, questStep.id)
    }
}
