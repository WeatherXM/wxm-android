package com.weatherxm.ui.signup

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.data.ApiError.AuthError.InvalidUsername
import com.weatherxm.data.ApiError.AuthError.SignupError.UserAlreadyExists
import com.weatherxm.data.Failure
import com.weatherxm.data.Resource
import com.weatherxm.usecases.AuthUseCase
import com.weatherxm.util.ResourcesHelper
import com.weatherxm.util.UIErrors.getDefaultMessage
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class SignupViewModel : ViewModel(), KoinComponent {

    private val authUseCase: AuthUseCase by inject()
    private val resHelper: ResourcesHelper by inject()

    private val isSignedUp = MutableLiveData<Resource<String>>()
    fun isSignedUp() = isSignedUp

    fun signup(username: String, firstName: String?, lastName: String?) {
        isSignedUp.postValue(Resource.loading())
        viewModelScope.launch {
            val first = if (firstName.isNullOrEmpty()) null else firstName
            val last = if (lastName.isNullOrEmpty()) null else lastName
            authUseCase.signup(username, first, last)
                .mapLeft {
                    Timber.d("Signup Error: $it")
                    handleFailure(it, username)
                }.map {
                    isSignedUp.postValue(
                        Resource.success(resHelper.getString(R.string.success_signup_text, it))
                    )
                }
        }
    }

    private fun handleFailure(failure: Failure, username: String) {
        isSignedUp.postValue(
            Resource.error(
                when (failure) {
                    is InvalidUsername -> resHelper.getString(
                        R.string.error_signup_invalid_username
                    )
                    is UserAlreadyExists -> resHelper.getString(
                        R.string.error_signup_user_already_exists,
                        username
                    )
                    else -> failure.getDefaultMessage()
                }
            )
        )
    }
}
