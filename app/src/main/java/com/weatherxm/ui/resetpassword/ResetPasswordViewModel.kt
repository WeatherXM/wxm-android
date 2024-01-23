package com.weatherxm.ui.resetpassword

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.data.ApiError.AuthError.InvalidUsername
import com.weatherxm.data.Failure
import com.weatherxm.data.Resource
import com.weatherxm.usecases.AuthUseCase
import com.weatherxm.util.Analytics
import com.weatherxm.util.Failure.getDefaultMessageResId
import com.weatherxm.util.Resources
import kotlinx.coroutines.launch

class ResetPasswordViewModel(
    private val authUseCase: AuthUseCase,
    private val resources: Resources,
    private val analytics: Analytics
) : ViewModel() {
    private val isEmailSent = MutableLiveData<Resource<Unit>>()
    fun isEmailSent() = isEmailSent

    fun resetPassword(email: String) {
        isEmailSent.postValue(Resource.loading())
        viewModelScope.launch {
            authUseCase.resetPassword(email)
                .mapLeft {
                    analytics.trackEventFailure(it.code)
                    handleFailure(it)
                }
                .map {
                    isEmailSent.postValue(Resource.success(Unit))
                }
        }
    }

    private fun handleFailure(failure: Failure) {
        isEmailSent.postValue(
            Resource.error(
                resources.getString(
                    if(failure is InvalidUsername) {
                        R.string.error_password_reset_invalid_username
                    } else {
                        failure.getDefaultMessageResId()
                    }
                )
            )
        )
    }
}
