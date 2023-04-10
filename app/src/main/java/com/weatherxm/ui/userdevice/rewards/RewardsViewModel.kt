package com.weatherxm.ui.userdevice.rewards

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.data.ApiError
import com.weatherxm.data.Device
import com.weatherxm.data.Failure
import com.weatherxm.data.NetworkError.ConnectionTimeoutError
import com.weatherxm.data.NetworkError.NoConnectionError
import com.weatherxm.ui.common.TokenInfo
import com.weatherxm.ui.common.UIError
import com.weatherxm.usecases.UserDeviceUseCase
import com.weatherxm.util.ResourcesHelper
import com.weatherxm.util.UIErrors.getDefaultMessage
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class RewardsViewModel(var device: Device) : ViewModel(), KoinComponent {
    private val resHelper: ResourcesHelper by inject()
    private val userDeviceUseCase: UserDeviceUseCase by inject()

    private val onLoading = MutableLiveData<Boolean>()

    private val onError = MutableLiveData<UIError>()

    private val onTokens = MutableLiveData<TokenInfo>()

    fun onLoading(): LiveData<Boolean> = onLoading

    fun onError(): LiveData<UIError> = onError

    fun onTokens(): LiveData<TokenInfo> = onTokens

    fun fetchTokenDetails() {
        onLoading.postValue(true)
        viewModelScope.launch {
            userDeviceUseCase.getTokenInfoLast30D(device.id)
                .map { onTokens.postValue(it) }
                .mapLeft { handleTokenFailure(it) }
            onLoading.postValue(false)
        }
    }

    private fun handleTokenFailure(failure: Failure) {
        val uiError = UIError("", null)
        when (failure) {
            is ApiError.GenericError -> {
                uiError.errorMessage =
                    failure.message ?: resHelper.getString(R.string.error_reach_out_short)
            }
            is NoConnectionError, is ConnectionTimeoutError -> {
                uiError.errorMessage = failure.getDefaultMessage(R.string.error_reach_out_short)
                uiError.retryFunction = ::fetchTokenDetails
            }
            else -> {
                uiError.errorMessage = resHelper.getString(R.string.error_reach_out_short)
            }
        }
        onError.postValue(uiError)
    }
}
