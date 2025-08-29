package com.weatherxm.ui.queststeps

import androidx.lifecycle.ViewModel
import com.weatherxm.ui.common.QuestStep
import com.weatherxm.usecases.QuestsUseCase
import com.weatherxm.data.datasource.QuestsDataSourceImpl.Companion.ONBOARDING_ID


class QuestGenericStepViewModel(
    val questStep: QuestStep,
    private val useCase: QuestsUseCase
): ViewModel() {

    fun markStepAsCompleted(userId: String) {
        useCase.markQuestStepAsCompleted(userId, ONBOARDING_ID,questStep.id)
    }

    fun markStepAsSkipped(userId: String) {
        useCase.markQuestStepAsSkipped(userId, ONBOARDING_ID,questStep.id)
    }
}
