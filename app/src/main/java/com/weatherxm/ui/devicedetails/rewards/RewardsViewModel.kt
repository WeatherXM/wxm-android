package com.weatherxm.ui.devicedetails.rewards

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.data.ApiError
import com.weatherxm.data.Failure
import com.weatherxm.data.NetworkError.ConnectionTimeoutError
import com.weatherxm.data.NetworkError.NoConnectionError
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.UIError
import com.weatherxm.ui.common.UIRewardObject
import com.weatherxm.ui.common.UIRewards
import com.weatherxm.usecases.DeviceDetailsUseCase
import com.weatherxm.util.Analytics
import com.weatherxm.util.ResourcesHelper
import com.weatherxm.util.UIErrors.getDefaultMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class RewardsViewModel(var device: UIDevice = UIDevice.empty()) : ViewModel(), KoinComponent {
    private val resHelper: ResourcesHelper by inject()
    private val deviceDetailsUseCase: DeviceDetailsUseCase by inject()
    private val analytics: Analytics by inject()

    enum class TabSelected(val analyticsValue: String) {
        LATEST(Analytics.ParamValue.LATEST.paramValue),
        LAST_WEEK(Analytics.ParamValue.DAYS_7.paramValue),
        LAST_MONTH(Analytics.ParamValue.DAYS_30.paramValue)
    }

    private var currentUIRewards: UIRewards? = null
    private var fetchRewardsJob: Job? = null

    private val onLoading = MutableLiveData<Boolean>()

    private val onError = MutableLiveData<UIError>()

    private val onTotalRewards = MutableLiveData<Float?>()
    private val onRewardsObject = MutableLiveData<UIRewardObject?>()

    fun onLoading(): LiveData<Boolean> = onLoading

    fun onError(): LiveData<UIError> = onError

    fun onRewardsObject(): LiveData<UIRewardObject?> = onRewardsObject
    fun onTotalRewards(): LiveData<Float?> = onTotalRewards

    fun fetchRewards(tabSelected: TabSelected = TabSelected.LATEST) {
        currentUIRewards?.let {
            when (tabSelected) {
                TabSelected.LATEST -> onRewardsObject.postValue(it.latest)
                TabSelected.LAST_WEEK -> onRewardsObject.postValue(it.weekly)
                TabSelected.LAST_MONTH -> onRewardsObject.postValue(it.monthly)
            }
        } ?: fetchRewardsFromNetwork(tabSelected)
    }

    fun fetchRewardsFromNetwork(tabSelected: TabSelected = TabSelected.LATEST) {
        fetchRewardsJob?.let {
            if (it.isActive) {
                it.cancel("Cancelling running history job.")
            }
        }

        fetchRewardsJob = viewModelScope.launch {
            onLoading.postValue(true)
            deviceDetailsUseCase.getRewards(device.id)
                .onRight {
                    currentUIRewards = it
                    onTotalRewards.postValue(it.allTimeRewards)
                    when (tabSelected) {
                        TabSelected.LATEST -> onRewardsObject.postValue(it.latest)
                        TabSelected.LAST_WEEK -> onRewardsObject.postValue(it.weekly)
                        TabSelected.LAST_MONTH -> onRewardsObject.postValue(it.monthly)
                    }
                }
                .onLeft {
                    analytics.trackEventFailure(it.code)
                    handleRewardsFailure(it, tabSelected)
                }
            onLoading.postValue(false)
        }
    }

    private fun handleRewardsFailure(failure: Failure, tabSelected: TabSelected) {
        val uiError = UIError("", null)
        when (failure) {
            is ApiError.GenericError -> {
                uiError.errorMessage =
                    failure.message ?: resHelper.getString(R.string.error_reach_out_short)
            }
            is NoConnectionError, is ConnectionTimeoutError -> {
                uiError.errorMessage = failure.getDefaultMessage(R.string.error_reach_out_short)
                uiError.retryFunction = { fetchRewardsFromNetwork(tabSelected) }
            }
            else -> {
                uiError.errorMessage = resHelper.getString(R.string.error_reach_out_short)
            }
        }
        onError.postValue(uiError)
    }
}
