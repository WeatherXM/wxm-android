package com.weatherxm.ui.deleteaccount

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.data.models.ApiError.AuthError.LoginError.InvalidCredentials
import com.weatherxm.data.models.ApiError.AuthError.LoginError.InvalidPassword
import com.weatherxm.ui.common.Resource
import com.weatherxm.ui.common.empty
import com.weatherxm.usecases.DeleteAccountUseCase
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.util.Failure.getCode
import com.weatherxm.util.Failure.getDefaultMessage
import com.weatherxm.util.Resources
import com.weatherxm.util.Validator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DeleteAccountViewModel(
    private val resources: Resources,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val analytics: AnalyticsWrapper
) : ViewModel() {

    private val onStatus = MutableLiveData<Resource<State>>()
    fun onStatus() = onStatus

    fun isOnSafeState(): Boolean {
        return onStatus.value?.status != com.weatherxm.ui.common.Status.LOADING
    }

    fun isAccountedDeleted(): Boolean {
        return onStatus.value?.data?.status == Status.ACCOUNT_DELETION
            && onStatus.value?.status == com.weatherxm.ui.common.Status.SUCCESS
    }

    @Suppress("MagicNumber")
    fun checkAndDeleteAccount(password: String) {
        if (!Validator.validatePassword(password)) {
            onStatus.postValue(
                Resource.error(
                    resources.getString(R.string.warn_invalid_password),
                    State(Status.PASSWORD_VERIFICATION, InvalidPassword(String.empty()))
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
                if (it is InvalidCredentials) {
                    onStatus.postValue(
                        Resource.error(
                            resources.getString(R.string.warn_invalid_password),
                            State(Status.PASSWORD_VERIFICATION, InvalidPassword(String.empty()))
                        )
                    )
                } else {
                    onStatus.postValue(
                        Resource.error(
                            it.getDefaultMessage(), State(Status.PASSWORD_VERIFICATION)
                        )
                    )
                }
            }
        }
    }

    private suspend fun deleteAccount() {
        onStatus.postValue(Resource.loading(State(Status.ACCOUNT_DELETION)))
        deleteAccountUseCase.deleteAccount()
            .onRight {
                analytics.onLogout()
                onStatus.postValue(Resource.success(State(Status.ACCOUNT_DELETION)))
            }
            .onLeft {
                analytics.trackEventFailure(it.code)
                onStatus.postValue(Resource.error(it.getCode(), State(Status.ACCOUNT_DELETION)))
            }
    }
}
