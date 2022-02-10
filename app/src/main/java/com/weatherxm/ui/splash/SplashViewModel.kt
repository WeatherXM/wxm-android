package com.weatherxm.ui.splash

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import com.weatherxm.data.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class SplashViewModel : ViewModel(), KoinComponent {

    private val repository: AuthRepository by inject()

    private val isLoggedIn = MutableLiveData<Either<Error, String>>().apply {
        Timber.d("Getting credentials in the background")
        viewModelScope.launch(Dispatchers.IO) {
            postValue(repository.isLoggedIn())
        }
    }

    fun isLoggedIn(): LiveData<Either<Error, String>> = isLoggedIn

}
