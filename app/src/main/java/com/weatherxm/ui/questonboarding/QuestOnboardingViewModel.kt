package com.weatherxm.ui.questonboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.data.datasource.QuestsDataSourceImpl.Companion.ONBOARDING_ID
import com.weatherxm.ui.common.QuestOnboardingData
import com.weatherxm.ui.common.SingleLiveEvent
import com.weatherxm.usecases.QuestsUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import timber.log.Timber

class QuestOnboardingViewModel(
    val userId: String,
    private val usecase: QuestsUseCase,
    private val dispatcher: CoroutineDispatcher
) : ViewModel() {
    private val _onError = SingleLiveEvent<Throwable>()
    private val _onDataLoaded = SingleLiveEvent<Unit>()
    private val _onQuestCompleted = SingleLiveEvent<Unit>()

    fun onError() = _onError
    fun onDataLoaded() = _onDataLoaded
    fun onQuestCompleted() = _onQuestCompleted

    var onboardingQuestData: QuestOnboardingData? = null

    fun getData() {
        viewModelScope.launch(dispatcher) {
            usecase.fetchOnboardingData(userId).onRight {
                onboardingQuestData = it
            }.onLeft {
                _onError.postValue(it)
            }

            _onDataLoaded.postValue(Unit)
        }
    }

    fun completeQuest() {
        viewModelScope.launch(dispatcher) {
            usecase.completeQuest(userId, ONBOARDING_ID).onRight {
                _onQuestCompleted.postValue(Unit)
            }.onLeft {
                Timber.e(it, "[Firestore]: Error when completing the onboarding quest")
                _onError.postValue(it)
            }
        }
    }
}
