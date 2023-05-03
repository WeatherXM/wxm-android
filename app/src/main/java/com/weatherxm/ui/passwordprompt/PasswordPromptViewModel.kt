package com.weatherxm.ui.passwordprompt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.data.Resource
import com.weatherxm.data.SingleLiveEvent
import com.weatherxm.usecases.PasswordPromptUseCase
import com.weatherxm.util.ResourcesHelper
import com.weatherxm.util.UIErrors.getDefaultMessage
import com.weatherxm.util.Validator
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PasswordPromptViewModel : ViewModel(), KoinComponent {
    private val usecase: PasswordPromptUseCase by inject()
    private val validator: Validator by inject()
    private val resHelper: ResourcesHelper by inject()

    private val onValidPassword = SingleLiveEvent<Resource<Unit>>()
    fun onValidPassword() = onValidPassword

    fun checkPassword(password: String) {
        onValidPassword.postValue(Resource.loading())

        // Check if the password is valid locally
        if (!validator.validatePassword(password)) {
            onValidPassword.postValue(
                Resource.error(resHelper.getString(R.string.error_invalid_password))
            )
            return
        }

        // Check with the server if the password is correct
        viewModelScope.launch {
            usecase.isPasswordCorrect(password).onRight {
                onValidPassword.postValue(Resource.success(Unit))
            }.onLeft {
                onValidPassword.postValue(
                    Resource.error(it.getDefaultMessage(R.string.error_invalid_password))
                )
            }
        }
    }
}
