package com.weatherxm.ui.devicesrewards

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.repository.RewardsRepositoryImpl
import com.weatherxm.ui.common.Contracts.LOADING_DELAY
import com.weatherxm.ui.common.DeviceTotalRewardsDetails
import com.weatherxm.ui.common.DevicesRewards
import com.weatherxm.ui.common.DevicesRewardsByRange
import com.weatherxm.ui.common.Resource
import com.weatherxm.ui.common.Status
import com.weatherxm.usecases.RewardsUseCase
import com.weatherxm.util.Failure.getDefaultMessage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DevicesRewardsViewModel(
    val rewards: DevicesRewards,
    val usecase: RewardsUseCase,
    private val analytics: AnalyticsWrapper,
    private val dispatcher: CoroutineDispatcher
) : ViewModel() {
    private val totalSelectedRangeChip = mutableIntStateOf(R.string.seven_days_abbr)
    private val totalRangeChipsToggleEnabled = mutableStateOf(true)

    fun totalSelectedRangeChip(): Int = totalSelectedRangeChip.intValue
    fun totalRangeChipsToggleEnabled(): Boolean = totalRangeChipsToggleEnabled.value

    private val devicesJobs = mutableMapOf<Int, Job>()

    /**
     * The Int here represents the position in the list/adapter
     */
    private val onDeviceRewardDetails = MutableLiveData<Pair<Int, DeviceTotalRewardsDetails>>()
    private val onRewardsByRange = MutableLiveData<Resource<DevicesRewardsByRange>>()

    fun onDeviceRewardDetails():
        LiveData<Pair<Int, DeviceTotalRewardsDetails>> = onDeviceRewardDetails

    fun onRewardsByRange(): LiveData<Resource<DevicesRewardsByRange>> = onRewardsByRange

    fun getDevicesRewardsByRangeTotals(selectedRangeChipId: Int = totalSelectedRangeChip.intValue) {
        totalSelectedRangeChip.intValue = selectedRangeChipId

        viewModelScope.launch(dispatcher) {
            onRewardsByRange.postValue(Resource.loading())
            totalRangeChipsToggleEnabled.value = false
            /**
             * Needed due to an issue with the chart drawing if the API replies very fast:
             * https://linear.app/weatherxm/issue/FE-1564/fix-chart-in-devices-rewards-showing-no-data-at-first-open
             */
            delay(LOADING_DELAY)
            val mode = labelResIdToMode(selectedRangeChipId)
            usecase.getDevicesRewardsByRange(mode).onRight {
                onRewardsByRange.postValue(Resource.success(it))
            }.onLeft {
                onRewardsByRange.postValue(Resource.error(it.getDefaultMessage()))
                analytics.trackEventFailure(it.code)
            }
            totalRangeChipsToggleEnabled.value = true
        }
    }

    fun getDeviceRewardsByRange(
        deviceId: String,
        position: Int,
        selectedRangeChipId: Int = R.string.seven_days_abbr
    ) {
        val job = viewModelScope.launch(dispatcher) {
            val selectedMode = labelResIdToMode(selectedRangeChipId)
            rewards.devices[position].details.status = Status.LOADING
            rewards.devices[position].details.mode = selectedMode
            onDeviceRewardDetails.postValue(Pair(position, rewards.devices[position].details))
            /**
             * Needed due to an issue with the chart drawing if the API replies very fast:
             * https://linear.app/weatherxm/issue/FE-1564/fix-chart-in-devices-rewards-showing-no-data-at-first-open
             */
            delay(LOADING_DELAY)
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

    private fun labelResIdToMode(
        labelResId: Int
    ): RewardsRepositoryImpl.Companion.RewardsSummaryMode {
        return when (labelResId) {
            R.string.seven_days_abbr -> RewardsRepositoryImpl.Companion.RewardsSummaryMode.WEEK
            R.string.one_month_abbr -> RewardsRepositoryImpl.Companion.RewardsSummaryMode.MONTH
            else -> throw NotImplementedError("Unknown chip ID $labelResId")
        }
    }
}
