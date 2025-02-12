package com.weatherxm.ui.rewardslist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.Failure
import com.weatherxm.ui.common.Resource
import com.weatherxm.ui.common.RewardTimelineType
import com.weatherxm.ui.common.TimelineReward
import com.weatherxm.usecases.RewardsUseCase
import com.weatherxm.util.Failure.getDefaultMessage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class RewardsListViewModel(
    private val usecase: RewardsUseCase,
    private val analytics: AnalyticsWrapper,
    private val dispatcher: CoroutineDispatcher
) : ViewModel() {
    private var currentPage = 0
    private var hasNextPage = false
    private var blockNewPageRequest = false
    private val currentShownRewards = mutableListOf<TimelineReward>()

    private val onFirstPageRewards = MutableLiveData<Resource<List<TimelineReward>>>()
    private val onNewRewardsPage = MutableLiveData<Resource<List<TimelineReward>>>()
    private val onEndOfData = MutableLiveData<List<TimelineReward>>()

    fun onFirstPageRewards(): LiveData<Resource<List<TimelineReward>>> = onFirstPageRewards
    fun onNewRewardsPage(): LiveData<Resource<List<TimelineReward>>> = onNewRewardsPage
    fun onEndOfData(): LiveData<List<TimelineReward>> = onEndOfData

    fun fetchFirstPageRewards(deviceId: String) {
        onFirstPageRewards.postValue(Resource.loading())
        viewModelScope.launch(dispatcher) {
            usecase.getRewardsTimeline(deviceId, currentPage).onRight {
                Timber.d("Got Rewards: ${it.rewards}")
                hasNextPage = it.hasNextPage
                currentShownRewards.addAll(it.rewards)
                onFirstPageRewards.postValue(Resource.success(currentShownRewards))
            }.onLeft {
                analytics.trackEventFailure(it.code)
                handleFailure(it)
            }
        }
    }

    fun fetchNewPageRewards(deviceId: String) {
        if (hasNextPage && !blockNewPageRequest) {
            onNewRewardsPage.postValue(Resource.loading())
            viewModelScope.launch(dispatcher) {
                currentPage++
                blockNewPageRequest = true

                usecase.getRewardsTimeline(deviceId, currentPage).onRight {
                    Timber.d("Got Rewards: ${it.rewards}")
                    hasNextPage = it.hasNextPage
                    currentShownRewards.addAll(it.rewards)
                    onNewRewardsPage.postValue(Resource.success(currentShownRewards))
                }.onLeft {
                    /**
                     * We came across an error so we should try again for this page
                     */
                    currentPage--
                    analytics.trackEventFailure(it.code)
                }

                blockNewPageRequest = false
            }
        } else if (!hasNextPage) {
            currentShownRewards.add(TimelineReward(RewardTimelineType.END_OF_LIST, null))
            onEndOfData.postValue(currentShownRewards)
        }
    }

    private fun handleFailure(failure: Failure) {
        onFirstPageRewards.postValue(Resource.error(failure.getDefaultMessage()))
    }
}
