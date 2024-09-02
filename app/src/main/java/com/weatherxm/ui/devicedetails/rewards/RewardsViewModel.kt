package com.weatherxm.ui.devicedetails.rewards

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.ApiError
import com.weatherxm.data.Failure
import com.weatherxm.data.NetworkError.ConnectionTimeoutError
import com.weatherxm.data.NetworkError.NoConnectionError
import com.weatherxm.data.Rewards
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.UIError
import com.weatherxm.ui.common.empty
import com.weatherxm.usecases.DeviceDetailsUseCase
import com.weatherxm.util.Failure.getDefaultMessage
import com.weatherxm.util.Resources
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class RewardsViewModel(
    var device: UIDevice = UIDevice.empty(),
    private val resources: Resources,
    private val deviceDetailsUseCase: DeviceDetailsUseCase,
    private val analytics: AnalyticsWrapper
) : ViewModel() {
    private var fetchRewardsJob: Job? = null

    private val onLoading = MutableLiveData<Boolean>()
    private val onError = MutableLiveData<UIError>()
    private val onRewards = MutableLiveData<Rewards>()

    fun onLoading(): LiveData<Boolean> = onLoading
    fun onError(): LiveData<UIError> = onError
    fun onRewards(): LiveData<Rewards> = onRewards

    fun fetchRewardsFromNetwork() {
        /**
         * If we got here directly from a notification,
         * then we need to wait for the View Model to load the device from the network,
         * then proceed in fetching the rewards
         */
        if (device.isEmpty()) {
            return
        }

        fetchRewardsJob?.let {
            if (it.isActive) {
                it.cancel("Cancelling running history job.")
            }
        }

        fetchRewardsJob = viewModelScope.launch {
            onLoading.postValue(true)
            deviceDetailsUseCase.getRewards(device.id)
                .onRight {
                    onRewards.postValue(it)
                }
                .onLeft {
                    analytics.trackEventFailure(it.code)
                    handleRewardsFailure(it)
                }
            onLoading.postValue(false)
        }
    }

    private fun handleRewardsFailure(failure: Failure) {
        val uiError = UIError(String.empty(), null)
        when (failure) {
            is ApiError.GenericError -> {
                uiError.errorMessage =
                    failure.message ?: resources.getString(R.string.error_reach_out_short)
            }
            is NoConnectionError, is ConnectionTimeoutError -> {
                uiError.errorMessage = failure.getDefaultMessage(R.string.error_reach_out_short)
                uiError.retryFunction = { fetchRewardsFromNetwork() }
            }
            else -> {
                uiError.errorMessage = resources.getString(R.string.error_reach_out_short)
            }
        }
        onError.postValue(uiError)
    }
}
