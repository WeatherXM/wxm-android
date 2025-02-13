package com.weatherxm.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.flatMap
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.ApiError.AuthError.InvalidUsername
import com.weatherxm.data.models.ApiError.AuthError.LoginError.InvalidCredentials
import com.weatherxm.data.models.ApiError.AuthError.LoginError.InvalidPassword
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.User
import com.weatherxm.ui.common.Resource
import com.weatherxm.usecases.AuthUseCase
import com.weatherxm.usecases.UserUseCase
import com.weatherxm.util.Failure.getDefaultMessage
import com.weatherxm.util.Failure.getDefaultMessageResId
import com.weatherxm.util.Resources
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import timber.log.Timber

class LoginViewModel(
    private val authUseCase: AuthUseCase,
    private val userUseCase: UserUseCase,
    private val resources: Resources,
    private val analytics: AnalyticsWrapper,
    private val dispatcher: CoroutineDispatcher
) : ViewModel() {

    private val onLogin = MutableLiveData<Resource<Unit>>()
    fun onLogin(): LiveData<Resource<Unit>> = onLogin

    private val user = MutableLiveData<Resource<User>>()
    fun user(): LiveData<Resource<User>> = user

    /**
     * Needed for checking if the user is logged in or not for a race condition where a user
     * that has added a widget, logs in to a 2nd account and taps the "Login" button in the Widget
     */
    fun isLoggedIn(): Boolean = authUseCase.isLoggedIn()

    fun login(username: String, password: String) {
        onLogin.postValue(Resource.loading())
        viewModelScope.launch(dispatcher) {
            authUseCase.login(username, password)
                .mapLeft {
                    analytics.trackEventFailure(it.code)
                    handleLoginFailure(it)
                    return@launch
                }
                .flatMap {
                    onLogin.postValue(Resource.success(Unit))
                    userUseCase.getUser()
                }
                .mapLeft {
                    analytics.trackEventFailure(it.code)
                    Timber.d("Got error when fetching the user on Login Screen: $it")
                    handleUserFailure(it)
                }
                .map {
                    analytics.setUserId(it.id)
                    user.postValue(Resource.success(it))
                }
        }
    }

    private fun handleLoginFailure(failure: Failure) {
        onLogin.postValue(
            Resource.error(
                resources.getString(
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

    fun shouldShowAnalyticsOptIn(): Boolean {
        return userUseCase.shouldShowAnalyticsOptIn()
    }
}
