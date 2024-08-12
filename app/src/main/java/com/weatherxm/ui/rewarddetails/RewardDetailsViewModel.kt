package com.weatherxm.ui.rewarddetails

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.ApiError
import com.weatherxm.data.Failure
import com.weatherxm.data.Resource
import com.weatherxm.data.RewardDetails
import com.weatherxm.data.RewardSplit
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.empty
import com.weatherxm.usecases.RewardsUseCase
import com.weatherxm.usecases.UserUseCase
import com.weatherxm.util.Failure.getDefaultMessage
import com.weatherxm.util.Resources
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.ZonedDateTime

class RewardDetailsViewModel(
    var device: UIDevice = UIDevice.empty(),
    private val analytics: AnalyticsWrapper,
    private val resources: Resources,
    private val usecase: RewardsUseCase,
    private val userUseCase: UserUseCase
) : ViewModel() {

    private val onRewardDetails = MutableLiveData<Resource<RewardDetails>>()

    fun onRewardDetails(): LiveData<Resource<RewardDetails>> = onRewardDetails

    private var rewardSplits: List<RewardSplit> = emptyList()
    private var walletAddressJob: Job? = null
    private var walletAddress = String.empty()

    private suspend fun fetchWalletAddress() {
        walletAddressJob = viewModelScope.launch {
            userUseCase.getWalletAddress().onRight {
                walletAddress = it
            }
        }
    }

    fun getWalletAddress(listener: (String) -> Unit) {
        viewModelScope.launch {
            if (walletAddressJob?.isActive == true) {
                walletAddressJob?.join()
            }
            listener(walletAddress)
        }
    }

    fun getRewardSplits() = rewardSplits

    fun fetchRewardDetails(timestamp: ZonedDateTime) {
        viewModelScope.launch {
            onRewardDetails.postValue(Resource.loading())

            usecase.getRewardDetails(device.id, timestamp)
                .onRight {
                    /**
                     * If it has split rewards, pre-fetch the wallet address as it might be required
                     * if user clicks "Show Split"
                     */
                    if (it.hasSplitRewards()) {
                        fetchWalletAddress()
                    }
                    rewardSplits = it.rewardSplit ?: emptyList()
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
