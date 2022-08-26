package com.weatherxm.ui.preferences

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import com.weatherxm.usecases.PreferencesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class PreferenceViewModel : ViewModel(), KoinComponent {
    private val preferencesUseCase: PreferencesUseCase by inject()

    // Needed for passing info to the activity to when logging out
    private val onLogout = MutableLiveData(false)

    // Needed for passing info to the fragment when user has clicked the start survey button
    private val onShowSurveyScreen = MutableLiveData(false)

    // Needed for passing info to the activity when the survey has been completed
    private val onDismissSurveyPrompt = MutableLiveData(false)

    fun onLogout() = onLogout
    fun onShowSurveyScreen() = onShowSurveyScreen
    fun onDismissSurveyPrompt() = onDismissSurveyPrompt

    fun isLoggedIn(): LiveData<Either<Error, String>> = isLoggedIn

    // Needed for checking if the user is logged in or not, so we can show/hide the logout button
    private val isLoggedIn = MutableLiveData<Either<Error, String>>().apply {
        Timber.d("Getting credentials in the background")
        viewModelScope.launch(Dispatchers.IO) {
            postValue(preferencesUseCase.isLoggedIn())
        }
    }

    fun showSurveyScreen() {
        onShowSurveyScreen.postValue(true)
    }

    fun logout() {
        viewModelScope.launch {
            preferencesUseCase.logout()
            onLogout.postValue(true)
        }
    }

    fun hasDismissedSurveyPrompt(): Boolean {
        return preferencesUseCase.hasDismissedSurveyPrompt()
    }

    fun dismissSurveyPrompt() {
        preferencesUseCase.dismissSurveyPrompt()
        onDismissSurveyPrompt.postValue(true)
    }
}
