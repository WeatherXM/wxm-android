package com.weatherxm.ui.preferences

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.Failure
import com.weatherxm.ui.common.empty
import com.weatherxm.usecases.PreferencesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class PreferenceViewModel(
    private val preferencesUseCase: PreferencesUseCase,
    private val analytics: AnalyticsWrapper
) : ViewModel() {
    val onPreferencesChanged = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        analytics.setUserProperties()
    }

    // Needed for passing info to the activity to when logging out
    private val onLogout = MutableLiveData(false)

    // Needed for passing info to the fragment when user has clicked the start survey button
    private val onShowSurveyScreen = MutableLiveData(false)

    // Needed for passing info to the activity when the survey has been completed
    private val onDismissSurveyPrompt = MutableLiveData(false)

    fun onLogout() = onLogout
    fun onShowSurveyScreen() = onShowSurveyScreen
    fun onDismissSurveyPrompt() = onDismissSurveyPrompt

    fun isLoggedIn(): LiveData<Either<Failure, Boolean>> = isLoggedIn

    // Needed for checking if the user is logged in or not, so we can show/hide the logout button
    private val isLoggedIn = MutableLiveData<Either<Failure, Boolean>>().apply {
        Timber.d("Checking if user is logged in in the background")
        viewModelScope.launch(Dispatchers.IO) {
            postValue(preferencesUseCase.isLoggedIn())
        }
    }

    fun showSurveyScreen() {
        onShowSurveyScreen.postValue(true)
    }

    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            analytics.setUserId(String.empty())
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

    fun setAnalyticsEnabled(enabled: Boolean) {
        preferencesUseCase.setAnalyticsEnabled(enabled)
        analytics.setAnalyticsEnabled(enabled)
    }

    fun getInstallationId(): String? = preferencesUseCase.getInstallationId()
}
