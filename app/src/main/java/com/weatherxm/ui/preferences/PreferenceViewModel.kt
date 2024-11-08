package com.weatherxm.ui.preferences

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.Failure
import com.weatherxm.usecases.PreferencesUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class PreferenceViewModel(
    private val preferencesUseCase: PreferencesUseCase,
    private val analytics: AnalyticsWrapper,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    val onPreferencesChanged = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        analytics.setUserProperties()
    }

    // Needed for passing info to the activity to when logging out
    private val onLogout = MutableLiveData(false)

    fun onLogout() = onLogout

    fun isLoggedIn(): LiveData<Either<Failure, Boolean>> = isLoggedIn

    // Needed for checking if the user is logged in or not, so we can show/hide the logout button
    private val isLoggedIn = MutableLiveData<Either<Failure, Boolean>>().apply {
        Timber.d("Checking if user is logged in in the background")
        viewModelScope.launch(dispatcher) {
            postValue(preferencesUseCase.isLoggedIn())
        }
    }

    fun logout() {
        viewModelScope.launch(dispatcher) {
            analytics.onLogout()
            preferencesUseCase.logout()
            onLogout.postValue(true)
        }
    }

    fun setAnalyticsEnabled(enabled: Boolean) {
        preferencesUseCase.setAnalyticsEnabled(enabled)
        analytics.setAnalyticsEnabled(enabled)
    }

    fun getInstallationId(): String? = preferencesUseCase.getInstallationId()
}
