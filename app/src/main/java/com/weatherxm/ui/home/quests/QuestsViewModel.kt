package com.weatherxm.ui.home.quests

import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.weatherxm.ui.common.QuestOnboardingData
import com.weatherxm.ui.common.SingleLiveEvent
import com.weatherxm.usecases.QuestsUseCase
import kotlinx.coroutines.CoroutineDispatcher
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

            usecase.fetchOnboardingData(userId).onRight {
                onboardingQuestData = it
            }.onLeft {
                _onError.postValue(it)
            }

            _onDataLoaded.postValue(Unit)
        }
    }
}
