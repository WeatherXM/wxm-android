package com.weatherxm.ui.devicedetails.rewards

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.NetworkError.ConnectionTimeoutError
import com.weatherxm.data.models.NetworkError.NoConnectionError
import com.weatherxm.data.models.Rewards
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.UIError
import com.weatherxm.usecases.DeviceDetailsUseCase
import com.weatherxm.util.Failure.getDefaultMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class RewardsViewModel(
    var device: UIDevice = UIDevice.empty(),
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
            deviceDetailsUseCase.getRewards(device.id).onRight {
                onRewards.postValue(it)
            }.onLeft {
                analytics.trackEventFailure(it.code)
                handleRewardsFailure(it)
            }
            onLoading.postValue(false)
        }
    }

    private fun handleRewardsFailure(failure: Failure) {
        val uiError = UIError(failure.getDefaultMessage(R.string.error_reach_out_short))
        if (failure is NoConnectionError || failure is ConnectionTimeoutError) {
            uiError.retryFunction = { fetchRewardsFromNetwork() }
        }
        onError.postValue(uiError)
    }
}
