package com.weatherxm.ui.preferences

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.usecases.PreferencesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PreferenceViewModel(
    private val preferencesUseCase: PreferencesUseCase,
    private val analytics: AnalyticsWrapper
) : ViewModel() {
    val onPreferencesChanged = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        analytics.setUserProperties()
    }

    // Needed for passing info to the activity to when logging out
    private val onLogout = MutableLiveData(false)

    fun onLogout(): LiveData<Boolean> = onLogout
    fun isLoggedIn(): Boolean = preferencesUseCase.isLoggedIn()

    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
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
