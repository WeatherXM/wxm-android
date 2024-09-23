package com.weatherxm.ui.rewardboosts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.data.models.ApiError
import com.weatherxm.data.models.BoostReward
import com.weatherxm.data.models.Failure
import com.weatherxm.ui.common.Resource
import com.weatherxm.ui.common.UIBoost
import com.weatherxm.ui.common.empty
import com.weatherxm.usecases.RewardsUseCase
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.util.Failure.getDefaultMessage
import com.weatherxm.util.Resources
import kotlinx.coroutines.launch
import timber.log.Timber

class RewardBoostViewModel(
    var deviceId: String = String.empty(),
    private val analytics: AnalyticsWrapper,
    private val resources: Resources,
    private val usecase: RewardsUseCase
) : ViewModel() {

    private val onBoostReward = MutableLiveData<Resource<UIBoost>>()

    fun onBoostReward(): LiveData<Resource<UIBoost>> = onBoostReward

    fun fetchRewardBoost(boostReward: BoostReward) {
        viewModelScope.launch {
            onBoostReward.postValue(Resource.loading())

            usecase.getBoostReward(deviceId, boostReward)
                .onRight {
                    onBoostReward.postValue(Resource.success(it))
                }
                .onLeft {
                    Timber.d("Error fetching reward boost:", it)
                    analytics.trackEventFailure(it.code)
                    handleRewardsFailure(it)
                }
        }
    }

    private fun handleRewardsFailure(failure: Failure) {
        onBoostReward.postValue(
            Resource.error(
                if (failure is ApiError.DeviceNotFound) {
                    resources.getString(R.string.error_device_not_found)
                } else {
                    failure.getDefaultMessage(R.string.error_reach_out_short)
                }
            )
        )
    }
}
