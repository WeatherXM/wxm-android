package com.weatherxm.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import arrow.core.flatMap
import com.weatherxm.R
import com.weatherxm.data.ApiError.AuthError.InvalidUsername
import com.weatherxm.data.ApiError.AuthError.LoginError.InvalidCredentials
import com.weatherxm.data.ApiError.AuthError.LoginError.InvalidPassword
import com.weatherxm.data.Failure
import com.weatherxm.data.Resource
import com.weatherxm.data.User
import com.weatherxm.usecases.AuthUseCase
import com.weatherxm.util.ResourcesHelper
import com.weatherxm.util.UIErrors.getDefaultMessage
import com.weatherxm.util.UIErrors.getDefaultMessageResId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class LoginViewModel : ViewModel(), KoinComponent {

    private val authUseCase: AuthUseCase by inject()
    private val resHelper: ResourcesHelper by inject()

    private val onLogin = MutableLiveData<Resource<Unit>>()
    fun onLogin() = onLogin

    private val user = MutableLiveData<Resource<User>>()
    fun user(): LiveData<Resource<User>> = user

    /*
    * Needed for checking if the user is logged in or not for a race condition where a user
    * that has added a widget, logs in to a 2nd account and taps the "Login" button in the Widget
     */
    private val isLoggedIn = MutableLiveData<Either<Failure, Boolean>>().apply {
        Timber.d("Checking if user is logged in in the background")
        viewModelScope.launch(Dispatchers.IO) {
            postValue(authUseCase.isLoggedIn())
        }
    }

    fun isLoggedIn(): LiveData<Either<Failure, Boolean>> = isLoggedIn

    fun login(username: String, password: String) {
        onLogin.postValue(Resource.loading())
        viewModelScope.launch {
            authUseCase.login(username, password)
                .mapLeft {
                    handleLoginFailure(it)
                    return@launch
                }
                .flatMap {
                    onLogin.postValue(Resource.success(Unit))
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
        onLogin.postValue(
            Resource.error(
                resHelper.getString(
                    when (failure) {
                        is InvalidUsername -> R.string.error_login_invalid_username
                        is InvalidPassword -> R.string.error_login_invalid_password
                        is InvalidCredentials -> R.string.error_login_invalid_credentials
                        else -> failure.getDefaultMessageResId(R.string.error_reach_out_short)
                    }
                )
            )
        )
    }

    private fun handleUserFailure(failure: Failure) {
        user.postValue(Resource.error(failure.getDefaultMessage()))
    }
}
