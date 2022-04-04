package com.weatherxm.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import arrow.core.flatMap
import com.weatherxm.R
import com.weatherxm.data.ApiError.AuthError.InvalidUsername
import com.weatherxm.data.ApiError.AuthError.LoginError.InvalidCredentials
import com.weatherxm.data.ApiError.AuthError.LoginError.InvalidPassword
import com.weatherxm.data.Failure
import com.weatherxm.data.Failure.NetworkError
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
                    handleLoginFailure(it)
                    return@launch
                }
                .flatMap {
                    isLoggedIn.postValue(Resource.success(Unit))
                    authUseCase.getUser()
                }
                .mapLeft {
                    Timber.d("Got error when fetching the user on Login Screen: $it")
                    handleUserFailure(it)
                }
                .map {
                    user.postValue(Resource.success(it))
                }
        }
    }

    private fun handleLoginFailure(failure: Failure) {
        isLoggedIn.postValue(
            Resource.error(
                resHelper.getString(
                    when (failure) {
                        is InvalidUsername -> R.string.error_login_invalid_username
                        is InvalidPassword -> R.string.error_login_invalid_password
                        is InvalidCredentials -> R.string.error_login_invalid_credentials
                        is NetworkError -> R.string.error_network
                        else -> R.string.error_unknown
                    }
                )
            )
        )
    }

    private fun handleUserFailure(failure: Failure) {
        user.postValue(
            Resource.error(
                resHelper.getString(
                    when (failure) {
                        is NetworkError -> R.string.error_network
                        else -> R.string.error_unknown
                    }
                )
            )
        )
    }
}
