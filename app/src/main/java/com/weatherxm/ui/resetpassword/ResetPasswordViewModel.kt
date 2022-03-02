package com.weatherxm.ui.resetpassword

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.weatherxm.R
import com.weatherxm.data.ApiError.AuthError.InvalidUsername
import com.weatherxm.data.Failure
import com.weatherxm.data.Failure.NetworkError
import com.weatherxm.data.Resource
import com.weatherxm.usecases.AuthUseCase
import com.weatherxm.util.ResourcesHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ResetPasswordViewModel : ViewModel(), KoinComponent {

    private val authUseCase: AuthUseCase by inject()
    private val resHelper: ResourcesHelper by inject()

    private val isEmailSent = MutableLiveData<Resource<Unit>>()
    fun isEmailSent() = isEmailSent

    fun resetPassword(email: String) {
        isEmailSent.postValue(Resource.loading())
        CoroutineScope(Dispatchers.IO).launch {
            authUseCase.resetPassword(email)
                .mapLeft {
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
                resHelper.getString(
                    when (failure) {
                        is InvalidUsername -> R.string.password_reset_invalid_username
                        is NetworkError -> R.string.network_error
                        else -> R.string.unknown_error
                    }
                )
            )
        )
    }
}
