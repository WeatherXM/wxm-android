package com.weatherxm.ui.signup

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.ApiError.AuthError.InvalidUsername
import com.weatherxm.data.models.ApiError.AuthError.SignupError.UserAlreadyExists
import com.weatherxm.data.models.Failure
import com.weatherxm.ui.common.Resource
import com.weatherxm.usecases.AuthUseCase
import com.weatherxm.util.Failure.getDefaultMessage
import com.weatherxm.util.Resources
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import timber.log.Timber

class SignupViewModel(
    private val authUseCase: AuthUseCase,
    private val resources: Resources,
    private val analytics: AnalyticsWrapper,
    private val dispatcher: CoroutineDispatcher
) : ViewModel() {

    private val isSignedUp = MutableLiveData<Resource<String>>()
    fun isSignedUp() = isSignedUp

    fun signup(username: String, firstName: String?, lastName: String?) {
        isSignedUp.postValue(Resource.loading())
        viewModelScope.launch(dispatcher) {
            authUseCase.signup(username, firstName, lastName)
                .onLeft {
                    analytics.trackEventFailure(it.code)
                    Timber.d("Signup Error: $it")
                    handleFailure(it, username)
                }
                .onRight {
                    isSignedUp.postValue(
                        Resource.success(resources.getString(R.string.success_signup_text, it))
                    )
                }
        }
    }

    private fun handleFailure(failure: Failure, username: String) {
        isSignedUp.postValue(
            Resource.error(
                when (failure) {
                    is InvalidUsername -> resources.getString(
                        R.string.error_signup_invalid_username
                    )
                    is UserAlreadyExists -> resources.getString(
                        R.string.error_signup_user_already_exists,
                        username
                    )
                    else -> failure.getDefaultMessage()
                }
            )
        )
    }
}
