package com.weatherxm.ui.preferences

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.ui.common.Resource
import com.weatherxm.usecases.AuthUseCase
import com.weatherxm.usecases.PreferencesUseCase
import com.weatherxm.util.Failure.getDefaultMessage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch

class PreferenceViewModel(
    private val preferencesUseCase: PreferencesUseCase,
    private val authUseCase: AuthUseCase,
    private val analytics: AnalyticsWrapper,
    private val dispatcher: CoroutineDispatcher
) : ViewModel() {
    val onPreferencesChanged = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        analytics.setUserProperties()
    }

    // Needed for passing info to the activity to when logging out
    private val onLogout = MutableLiveData<Resource<Unit>>()

    fun onLogout(): LiveData<Resource<Unit>> = onLogout
    fun isLoggedIn(): Boolean = authUseCase.isLoggedIn()

    fun logout() {
        viewModelScope.launch(dispatcher) {
            onLogout.postValue(Resource.loading())
            authUseCase.logout().onRight {
                analytics.onLogout()
                onLogout.postValue(Resource.success(Unit))
            }.onLeft {
                onLogout.postValue(Resource.error(it.getDefaultMessage()))
            }
        }
    }

    fun setAnalyticsEnabled(enabled: Boolean) {
        preferencesUseCase.setAnalyticsEnabled(enabled)
        analytics.setAnalyticsEnabled(enabled)
    }

    fun getInstallationId(): String? = preferencesUseCase.getInstallationId()
}
