package com.weatherxm.ui.login

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.weatherxm.data.Resource
import com.weatherxm.data.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class LoginViewModel : ViewModel(), KoinComponent {

    private val repository: AuthRepository by inject()

    private val isLoggedIn = MutableLiveData<Resource<Unit>>()
    fun isLoggedIn() = isLoggedIn

    fun login(username: String, password: String) {
        isLoggedIn.postValue(Resource.loading())
        CoroutineScope(Dispatchers.IO).launch {
            repository.login(username, password)
                .mapLeft {
                    Timber.d(it, "Login Error")
                    isLoggedIn.postValue(Resource.error("Login Error. ${it.message}"))
                }.map {
                    isLoggedIn.postValue(Resource.success(Unit))
                }
        }
    }
}
