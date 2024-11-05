package com.weatherxm.ui.devicesrewards

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.repository.RewardsRepositoryImpl
import com.weatherxm.ui.common.DeviceTotalRewardsDetails
import com.weatherxm.ui.common.DevicesRewards
import com.weatherxm.ui.common.DevicesRewardsByRange
import com.weatherxm.ui.common.Resource
import com.weatherxm.ui.common.Status
import com.weatherxm.usecases.RewardsUseCase
import com.weatherxm.util.Failure.getDefaultMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class DevicesRewardsViewModel(
    val rewards: DevicesRewards,
    val usecase: RewardsUseCase,
    private val analytics: AnalyticsWrapper
) : ViewModel() {
    private val devicesJobs = mutableMapOf<Int, Job>()

    /**
     * The Int here represents the position in the list/adapter
     */
    private val onDeviceRewardDetails = MutableLiveData<Pair<Int, DeviceTotalRewardsDetails>>()
    private val onRewardsByRange = MutableLiveData<Resource<DevicesRewardsByRange>>()

    fun onDeviceRewardDetails():
        LiveData<Pair<Int, DeviceTotalRewardsDetails>> = onDeviceRewardDetails

    fun onRewardsByRange(): LiveData<Resource<DevicesRewardsByRange>> = onRewardsByRange

    fun getDevicesRewardsByRangeTotals(checkedRangeChipId: Int = R.id.week) {
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

    fun getDeviceRewardsByRange(
        deviceId: String,
        position: Int,
        checkedRangeChipId: Int = R.id.week
    ) {
        val job = viewModelScope.launch {
            val selectedMode = chipToMode(checkedRangeChipId)
            rewards.devices[position].details.status = Status.LOADING
            rewards.devices[position].details.mode = selectedMode
            onDeviceRewardDetails.postValue(Pair(position, rewards.devices[position].details))

            usecase.getDeviceRewardsByRange(deviceId, selectedMode).onRight {
                rewards.devices[position].details = it
                onDeviceRewardDetails.postValue(Pair(position, it))
            }.onLeft { failure ->
                val erroneousDetails = DeviceTotalRewardsDetails.empty().apply {
                    this.mode = selectedMode
                    this.status = Status.ERROR
                }
                rewards.devices[position].details = erroneousDetails
                onDeviceRewardDetails.postValue(Pair(position, erroneousDetails))
                analytics.trackEventFailure(failure.code)
            }
        }
        devicesJobs[position] = job
    }

    fun cancelFetching(position: Int) {
        devicesJobs[position]?.cancel()
    }

    private fun chipToMode(chipId: Int): RewardsRepositoryImpl.Companion.RewardsSummaryMode {
        return when (chipId) {
            R.id.week -> RewardsRepositoryImpl.Companion.RewardsSummaryMode.WEEK
            R.id.month -> RewardsRepositoryImpl.Companion.RewardsSummaryMode.MONTH
            else -> throw NotImplementedError("Unknown chip ID $chipId")
        }
    }

}
