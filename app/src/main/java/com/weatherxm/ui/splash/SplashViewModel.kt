package com.weatherxm.ui.splash

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.weatherxm.data.repository.AppConfigRepository
import com.weatherxm.data.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class SplashViewModel : ViewModel(), KoinComponent {
    private val authRepository: AuthRepository by inject()
    private val appConfigRepository: AppConfigRepository by inject()
    private val remoteConfig: FirebaseRemoteConfig by inject()

    private val isLoggedIn = MutableLiveData<Either<Error, String>>()
    private val shouldUpdate = MutableLiveData(false)

    fun getFirebaseRemoteConfig(): FirebaseRemoteConfig {
        return remoteConfig
    }

    fun isLoggedIn(): LiveData<Either<Error, String>> = isLoggedIn
    fun shouldUpdate() = shouldUpdate

    fun checkIfLoggedIn() {
        Timber.d("Getting credentials in the background")
        viewModelScope.launch(Dispatchers.IO) {
            isLoggedIn.postValue(authRepository.isLoggedIn())
        }
    }

    fun shouldPromptUpdate() {
        if (appConfigRepository.shouldUpdate()) {
            appConfigRepository.setLastRemindedVersion()
            shouldUpdate.postValue(true)
        } else {
            checkIfLoggedIn()
        }
    }
}
