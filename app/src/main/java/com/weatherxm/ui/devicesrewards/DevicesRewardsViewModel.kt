package com.weatherxm.ui.devicesrewards

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.Resource
import com.weatherxm.data.repository.RewardsRepositoryImpl
import com.weatherxm.ui.common.DeviceTotalRewardsDetails
import com.weatherxm.ui.common.DevicesRewards
import com.weatherxm.ui.common.DevicesRewardsByRange
import com.weatherxm.usecases.RewardsUseCase
import com.weatherxm.util.Failure.getDefaultMessage
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
    private val onRewardsByRange = MutableLiveData<Resource<DevicesRewardsByRange>>()

    fun onDeviceRewardDetails():
        LiveData<Pair<Int, DeviceTotalRewardsDetails>> = onDeviceRewardDetails

    fun onRewardsByRange(): LiveData<Resource<DevicesRewardsByRange>> = onRewardsByRange

    fun getDevicesRewardsByRangeTotals(checkedRangeChipId: Int? = null) {
        viewModelScope.launch {
            onRewardsByRange.postValue(Resource.loading())

            val mode = chipToMode(checkedRangeChipId)
            usecase.getDevicesRewardsByRange(mode).onRight {
                onRewardsByRange.postValue(Resource.success(it))
            }.onLeft {
                onRewardsByRange.postValue(Resource.error(it.getDefaultMessage()))
                analytics.trackEventFailure(it.code)
            }
        }
    }

    fun getDeviceRewardsByRange(deviceId: String, position: Int, checkedRangeChipId: Int? = null) {
        viewModelScope.launch {
            val mode = chipToMode(checkedRangeChipId)

            usecase.getDeviceRewardsByRange(deviceId, mode).onRight {
                rewards.devices[position].details = it
                onDeviceRewardDetails.postValue(Pair(position, it))
            }.onLeft { failure ->
                val erroneousDetails = DeviceTotalRewardsDetails(
                    null,
                    null,
                    null,
                    mutableListOf(),
                    null,
                    null,
                    null,
                    true
                )
                rewards.devices[position].details = erroneousDetails
                onDeviceRewardDetails.postValue(Pair(position, erroneousDetails))
                analytics.trackEventFailure(failure.code)
            }
        }
    }

    private fun chipToMode(chipId: Int?): RewardsRepositoryImpl.Companion.RewardsSummaryMode {
        return when (chipId) {
            R.id.week -> RewardsRepositoryImpl.Companion.RewardsSummaryMode.WEEK
            R.id.month -> RewardsRepositoryImpl.Companion.RewardsSummaryMode.MONTH
            R.id.year -> RewardsRepositoryImpl.Companion.RewardsSummaryMode.YEAR
            else -> RewardsRepositoryImpl.Companion.RewardsSummaryMode.WEEK
        }
    }

}
