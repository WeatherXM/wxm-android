package com.weatherxm.ui.preferences

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import com.weatherxm.usecases.AuthUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class PreferenceViewModel : ViewModel(), KoinComponent {
    private val authUseCase: AuthUseCase by inject()

    // Needed for passing info to the activity to when logging out
    private val onLogout = MutableLiveData(false)

    fun onLogout() = onLogout

    // Needed for checking if the user is logged in or not, so we can show/hide the logout button
    private val isLoggedIn = MutableLiveData<Either<Error, String>>().apply {
        Timber.d("Getting credentials in the background")
        viewModelScope.launch(Dispatchers.IO) {
            postValue(authUseCase.isLoggedIn())
        }
    }

    fun logout() {
        CoroutineScope(Dispatchers.IO).launch {
            authUseCase.logout()
            onLogout.postValue(true)
        }
    }

    fun isLoggedIn(): LiveData<Either<Error, String>> = isLoggedIn

}
