package com.weatherxm.ui.passwordprompt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.data.Resource
import com.weatherxm.data.SingleLiveEvent
import com.weatherxm.usecases.PasswordPromptUseCase
import com.weatherxm.util.Analytics
import com.weatherxm.util.Failure.getDefaultMessage
import com.weatherxm.util.Resources
import com.weatherxm.util.Validator
import kotlinx.coroutines.launch

class PasswordPromptViewModel(
    private val usecase: PasswordPromptUseCase,
    private val resources: Resources,
    private val analytics: Analytics
) : ViewModel() {

    private val onValidPassword = SingleLiveEvent<Resource<Unit>>()
    fun onValidPassword() = onValidPassword

    fun checkPassword(password: String) {
        onValidPassword.postValue(Resource.loading())

        // Check if the password is valid locally
        if (!Validator.validatePassword(password)) {
            onValidPassword.postValue(
                Resource.error(resources.getString(R.string.error_invalid_password))
            )
            return
        }

        // Check with the server if the password is correct
        viewModelScope.launch {
            usecase.isPasswordCorrect(password).onRight {
                onValidPassword.postValue(Resource.success(Unit))
            }.onLeft {
                analytics.trackEventFailure(it.code)
                onValidPassword.postValue(
                    Resource.error(it.getDefaultMessage(R.string.error_invalid_password))
                )
            }
        }
    }
}
