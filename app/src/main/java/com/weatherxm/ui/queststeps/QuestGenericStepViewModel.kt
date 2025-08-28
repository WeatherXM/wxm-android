package com.weatherxm.ui.queststeps

import androidx.lifecycle.ViewModel
import com.weatherxm.ui.common.QuestStep
import com.weatherxm.usecases.QuestsUseCase

class QuestGenericStepViewModel(
    val questStep: QuestStep,
    private val useCase: QuestsUseCase
): ViewModel() {

    fun markStepAsCompleted(userId: String) {
        useCase.markOnboardingStepAsCompleted(userId, questStep.id)
    }
    
    fun markStepAsSkipped(userId: String) {
        useCase.markOnboardingStepAsSkipped(userId, questStep.id)
    }
}
