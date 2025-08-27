package com.weatherxm.ui.home.quests

import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.weatherxm.data.models.QuestUserProgress
import com.weatherxm.ui.common.QuestOnboardingData
import com.weatherxm.ui.common.SingleLiveEvent
import com.weatherxm.usecases.QuestsUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import timber.log.Timber

class QuestsViewModel(
    private val usecase: QuestsUseCase,
    private val dispatcher: CoroutineDispatcher
) : ViewModel() {
    private val _onError = SingleLiveEvent<Throwable>()
    private val _onDataLoaded = SingleLiveEvent<Unit>()

    fun onError() = _onError
    fun onDataLoaded() = _onDataLoaded
    val onQuestToggleOption = mutableIntStateOf(0)

    var user: FirebaseUser? = null
    var onboardingQuestData: QuestOnboardingData? = null

    fun getData() {
        viewModelScope.launch(dispatcher) {
            val userId = user?.uid ?: run {
                Timber.e("[Firestore]: No user ID: null")
                _onError.postValue(Exception("No user ID: null"))
                return@launch
            }

            usecase.fetchUser(userId).onLeft {
                Timber.e(it, "[Firestore]: Error when fetching user")
                _onError.postValue(it)
                return@launch
            }

            /**
             * Fetch onboarding progress and quests in parallel
             */
            val onboardingProgressDeferred = async { usecase.fetchOnboardingProgress(userId) }
            val onboardingQuestDeferred = async { usecase.fetchOnboardingQuest() }

            val onboardingProgressResult = onboardingProgressDeferred.await()
            val onboardingQuestResult = onboardingQuestDeferred.await()

            /**
             * Handle both results, if any fails, show an error.
             */
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
