package com.weatherxm.ui.rewarddetails

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.data.ApiError
import com.weatherxm.data.Failure
import com.weatherxm.data.Resource
import com.weatherxm.data.RewardDetails
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.usecases.RewardsUseCase
import com.weatherxm.util.Analytics
import com.weatherxm.util.Failure.getDefaultMessage
import com.weatherxm.util.Resources
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.ZonedDateTime

class RewardDetailsViewModel(
    var device: UIDevice = UIDevice.empty(),
    private val analytics: Analytics,
    private val resources: Resources,
    private val usecase: RewardsUseCase
) : ViewModel() {

    private val onRewardDetails = MutableLiveData<Resource<RewardDetails>>()

    fun onRewardDetails(): LiveData<Resource<RewardDetails>> = onRewardDetails
    fun getRewardsHideAnnotationThreshold() = usecase.getRewardsHideAnnotationThreshold()

    fun fetchRewardDetails(timestamp: ZonedDateTime) {
        viewModelScope.launch {
            onRewardDetails.postValue(Resource.loading())

            usecase.getRewardDetails(device.id, timestamp)
                .onRight {
                    onRewardDetails.postValue(Resource.success(it))
                }
                .onLeft {
                    Timber.d("Error fetching reward details:", it)
                    analytics.trackEventFailure(it.code)
                    handleRewardsFailure(it)
                }
        }
    }

    private fun handleRewardsFailure(failure: Failure) {
        onRewardDetails.postValue(
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
