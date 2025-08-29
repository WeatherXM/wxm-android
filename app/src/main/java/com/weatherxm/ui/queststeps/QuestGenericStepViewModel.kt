package com.weatherxm.ui.queststeps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.ui.common.QuestStep
import com.weatherxm.usecases.QuestsUseCase
import com.weatherxm.data.datasource.QuestsDataSourceImpl.Companion.ONBOARDING_ID
import com.weatherxm.ui.common.SingleLiveEvent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import timber.log.Timber


class QuestGenericStepViewModel(
    val questStep: QuestStep,
    val userId: String,
    private val useCase: QuestsUseCase,
    private val dispatcher: CoroutineDispatcher
): ViewModel() {

    fun markStepAsCompleted(completion: (Throwable?) -> Unit) {
        viewModelScope.launch(dispatcher) {
            useCase.markQuestStepAsCompleted(
                userId,
                ONBOARDING_ID,
                questStep.id
            )
                .onRight {
                    completion(null)
                }.onLeft {
                    Timber.e(it, "[Firestore]: Error when marking the step as completed")
                    completion(it)
                }
        }
    }

    fun markStepAsSkipped(completion: (Throwable?) -> Unit) {
        viewModelScope.launch(dispatcher) {
            useCase.markQuestStepAsSkipped(userId, ONBOARDING_ID, questStep.id)
                .onRight {
                    completion(null)
                }.onLeft {
                    Timber.e(it, "[Firestore]: Error when marking the step as completed")
                    completion(it)
                }
        }
    }
}
