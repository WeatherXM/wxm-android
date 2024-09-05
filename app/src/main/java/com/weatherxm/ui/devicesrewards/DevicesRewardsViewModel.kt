package com.weatherxm.ui.devicesrewards

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.repository.RewardsRepositoryImpl
import com.weatherxm.ui.common.DeviceTotalRewardsBoost
import com.weatherxm.ui.common.DeviceTotalRewardsDetails
import com.weatherxm.ui.common.DevicesRewards
import com.weatherxm.usecases.RewardsUseCase
import kotlinx.coroutines.launch

class DevicesRewardsViewModel(
    val rewards: DevicesRewards,
    val usecase: RewardsUseCase,
    private val analytics: AnalyticsWrapper
) : ViewModel() {

    /**
     * The Int here represents the position in the list/adapter
     */
    private val onDeviceRewardDetails = MutableLiveData<Pair<Int, DeviceTotalRewardsDetails>>()
    fun onDeviceRewardDetails():
        LiveData<Pair<Int, DeviceTotalRewardsDetails>> = onDeviceRewardDetails

    fun getDeviceRewardsSummary(deviceId: String, position: Int, checkedRangeChipId: Int? = null) {
        viewModelScope.launch {
            val mode = when (checkedRangeChipId) {
                R.id.week -> RewardsRepositoryImpl.Companion.RewardsSummaryMode.WEEK
                R.id.month -> RewardsRepositoryImpl.Companion.RewardsSummaryMode.MONTH
                R.id.year -> RewardsRepositoryImpl.Companion.RewardsSummaryMode.YEAR
                else -> RewardsRepositoryImpl.Companion.RewardsSummaryMode.WEEK
            }

            usecase.getDeviceRewardsSummary(deviceId, mode).onRight { summary ->
                val deviceTotalRewardDetails = DeviceTotalRewardsDetails(
                    summary.total,
                    mode,
                    summary.details?.map {
                        DeviceTotalRewardsBoost(
                            it.code,
                            it.completedPercentage?.toInt(),
                            it.totalRewards,
                            it.currentRewards,
                            it.boostPeriodStart,
                            it.boostPeriodEnd
                        )
                    },
                    false
                )
                rewards.devices[position].details = deviceTotalRewardDetails
                onDeviceRewardDetails.postValue(Pair(position, deviceTotalRewardDetails))
            }.onLeft { failure ->
                val erroneousDetails = DeviceTotalRewardsDetails(null, null, null, true)
                rewards.devices[position].details = erroneousDetails
                onDeviceRewardDetails.postValue(Pair(position, erroneousDetails))
                analytics.trackEventFailure(failure.code)
            }
        }
    }
}
