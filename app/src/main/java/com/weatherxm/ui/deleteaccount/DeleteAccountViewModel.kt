package com.weatherxm.ui.deleteaccount

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.data.ApiError.AuthError.LoginError.InvalidCredentials
import com.weatherxm.data.ApiError.AuthError.LoginError.InvalidPassword
import com.weatherxm.data.Resource
import com.weatherxm.usecases.DeleteAccountUseCase
import com.weatherxm.util.Analytics
import com.weatherxm.util.ResourcesHelper
import com.weatherxm.util.UIErrors.getCode
import com.weatherxm.util.UIErrors.getDefaultMessage
import com.weatherxm.util.Validator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DeleteAccountViewModel : ViewModel(), KoinComponent {
    private val validator: Validator by inject()
    private val resHelper: ResourcesHelper by inject()
    private val deleteAccountUseCase: DeleteAccountUseCase by inject()
    private val analytics: Analytics by inject()

    private val onStatus = MutableLiveData<Resource<State>>()
    fun onStatus() = onStatus

    fun isOnSafeState(): Boolean {
        return onStatus.value?.status != com.weatherxm.data.Status.LOADING
    }

    fun isAccountedDeleted(): Boolean {
        return onStatus.value?.data?.status == Status.ACCOUNT_DELETION
            && onStatus.value?.status == com.weatherxm.data.Status.SUCCESS
    }

    @Suppress("MagicNumber")
    fun checkAndDeleteAccount(password: String) {
        if (!validator.validatePassword(password)) {
            onStatus.postValue(
                Resource.error(
                    resHelper.getString(R.string.warn_invalid_password),
                    State(Status.PASSWORD_VERIFICATION, InvalidPassword(""))
                )
            )
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            onStatus.postValue(Resource.loading(State(Status.PASSWORD_VERIFICATION)))
            // Intentional delay for UX purposes
            delay(1000L)
            deleteAccountUseCase.isPasswordCorrect(password).onRight {
                deleteAccount()
            }.onLeft {
                analytics.trackEventFailure(it.code)
                when (it) {
                    is InvalidCredentials -> {
                        onStatus.postValue(
                            Resource.error(
                                resHelper.getString(R.string.warn_invalid_password),
                                State(Status.PASSWORD_VERIFICATION, InvalidPassword(""))
                            )
                        )
                    }
                    else -> {
                        onStatus.postValue(
                            Resource.error(
                                it.getDefaultMessage(), State(Status.PASSWORD_VERIFICATION)
                            )
                        )
                    }
                }
            }
        }
    }

    private suspend fun deleteAccount() {
        onStatus.postValue(Resource.loading(State(Status.ACCOUNT_DELETION)))
        deleteAccountUseCase.deleteAccount()
            .onRight {
                onStatus.postValue(Resource.success(State(Status.ACCOUNT_DELETION)))
            }
            .onLeft {
                analytics.trackEventFailure(it.code)
                onStatus.postValue(Resource.error(it.getCode(), State(Status.ACCOUNT_DELETION)))
            }
    }
}
