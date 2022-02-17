package com.weatherxm.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import arrow.core.flatMap
import com.weatherxm.R
import com.weatherxm.data.Resource
import com.weatherxm.data.User
import com.weatherxm.usecases.AuthUseCase
import com.weatherxm.util.ResourcesHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class LoginViewModel : ViewModel(), KoinComponent {

    private val authUseCase: AuthUseCase by inject()
    private val resHelper: ResourcesHelper by inject()

    private val isLoggedIn = MutableLiveData<Resource<Unit>>()
    fun isLoggedIn() = isLoggedIn

    private val user = MutableLiveData<Resource<User>>()
    fun user(): LiveData<Resource<User>> = user

    fun login(username: String, password: String) {
        isLoggedIn.postValue(Resource.loading())
        CoroutineScope(Dispatchers.IO).launch {
            authUseCase.login(username, password)
                .mapLeft {
                    Timber.d(it, "Login Error")
                    isLoggedIn.postValue(Resource.error("Login Error. ${it.message}"))
                }
                .flatMap {
                    isLoggedIn.postValue(Resource.success(Unit))
                    authUseCase.getUser()
                }
                .mapLeft {
                    Timber.d("Got error when fetching the user on Login Screen: $it")
                    user.postValue(
                        Resource.error(resHelper.getString(R.string.user_info_error))
                    )
                }
                .map {
                    user.postValue(Resource.success(it))
                }
        }
    }
}
