package com.weatherxm.ui.questonboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.data.models.QuestUserProgress
import com.weatherxm.ui.common.QuestOnboardingData
import com.weatherxm.ui.common.SingleLiveEvent
import com.weatherxm.usecases.QuestsUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import timber.log.Timber

class QuestOnboardingViewModel(
    private val userId: String,
    private val usecase: QuestsUseCase,
    private val dispatcher: CoroutineDispatcher
) : ViewModel() {
    private val _onError = SingleLiveEvent<Throwable>()
    private val _onDataLoaded = SingleLiveEvent<Unit>()

    fun onError() = _onError
    fun onDataLoaded() = _onDataLoaded

    var onboardingQuestData: QuestOnboardingData? = null

    fun getData() {
        viewModelScope.launch(dispatcher) {
            /**
             * Fetch onboarding progress and onboarding quest data in parallel
             */
            val onboardingProgressDeferred = async { usecase.fetchOnboardingProgress(userId) }
            val onboardingQuestDeferred = async { usecase.fetchOnboardingQuest() }

            val onboardingProgressResult = onboardingProgressDeferred.await()
            val onboardingQuestResult = onboardingQuestDeferred.await()

            var onboardingProgress: QuestUserProgress? = null
            onboardingProgressResult.onRight {
                onboardingProgress = it
            }.onLeft {
                Timber.e(it, "[Firestore]: Error when fetching user's onboarding progress")
                _onError.postValue(it)
            }
            onboardingQuestResult.onRight {
                onboardingQuestData = QuestOnboardingData(it, onboardingProgress)
            }.onLeft {
                Timber.e(it, "[Firestore]: Error when fetching the onboarding quest")
                _onError.postValue(it)
            }

            _onDataLoaded.postValue(Unit)
        }
    }
}
