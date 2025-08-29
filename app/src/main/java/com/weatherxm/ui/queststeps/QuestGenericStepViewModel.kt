package com.weatherxm.ui.queststeps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.data.datasource.QuestsDataSourceImpl.Companion.ONBOARDING_ID
import com.weatherxm.ui.common.QuestStep
import com.weatherxm.ui.common.SingleLiveEvent
import com.weatherxm.usecases.QuestsUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import timber.log.Timber

class QuestGenericStepViewModel(
    val questStep: QuestStep,
    val userId: String,
    private val useCase: QuestsUseCase,
    private val dispatcher: CoroutineDispatcher
) : ViewModel() {
    private val _onStepCompleted = SingleLiveEvent<Throwable?>()
    private val _onStepSkipped = SingleLiveEvent<Throwable?>()

    fun onStepCompleted(): SingleLiveEvent<Throwable?> = _onStepCompleted
    fun onStepSkipped(): SingleLiveEvent<Throwable?> = _onStepSkipped

    fun markStepAsCompleted() {
        viewModelScope.launch(dispatcher) {
            useCase.markQuestStepAsCompleted(userId, ONBOARDING_ID, questStep.id)
                .onRight {
                    _onStepCompleted.postValue(null)
                }
                .onLeft {
                    Timber.e(it, "[Firestore]: Error when marking the step as completed")
                    _onStepCompleted.postValue(it)
                }
        }
    }

    fun markStepAsSkipped() {
        viewModelScope.launch(dispatcher) {
            useCase.markQuestStepAsSkipped(userId, ONBOARDING_ID, questStep.id)
                .onRight {
                    _onStepSkipped.postValue(null)
                }
                .onLeft {
                    Timber.e(it, "[Firestore]: Error when marking the step as completed")
                    _onStepSkipped.postValue(it)
                }
        }
    }
}
