package com.weatherxm.ui.updateprompt

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import com.weatherxm.data.repository.AppConfigRepository
import com.weatherxm.data.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class UpdatePromptViewModel : ViewModel(), KoinComponent {

    private val authRepository: AuthRepository by inject()
    private val appConfigRepository: AppConfigRepository by inject()

    private val isLoggedIn = MutableLiveData<Either<Error, String>>()

    fun isLoggedIn(): LiveData<Either<Error, String>> = isLoggedIn

    fun isUpdateMandatory(): Boolean {
        return appConfigRepository.isUpdateMandatory()
    }

    fun getChangelog(): String {
        return appConfigRepository.getChangelog()
    }

    fun checkIfLoggedIn() {
        Timber.d("Getting credentials in the background")
        viewModelScope.launch(Dispatchers.IO) {
            isLoggedIn.postValue(authRepository.isLoggedIn())
        }
    }
}
