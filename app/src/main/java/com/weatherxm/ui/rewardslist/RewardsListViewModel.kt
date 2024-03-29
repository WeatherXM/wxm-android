package com.weatherxm.ui.rewardslist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.data.Failure
import com.weatherxm.data.Resource
import com.weatherxm.data.Reward
import com.weatherxm.usecases.RewardsUseCase
import com.weatherxm.util.Analytics
import com.weatherxm.util.Failure.getDefaultMessage
import kotlinx.coroutines.launch
import timber.log.Timber

class RewardsListViewModel(
    private val usecase: RewardsUseCase,
    private val analytics: Analytics
) : ViewModel() {
    private var currentPage = 0
    private var hasNextPage = false
    private var blockNewPageRequest = false
    private val currentShownRewards = mutableListOf<Reward>()

    private val onFirstPageRewards = MutableLiveData<Resource<List<Reward>>>().apply {
        value = Resource.loading()
    }
    private val onNewRewardsPage = MutableLiveData<Resource<List<Reward>>>()
    private val onEndOfData = MutableLiveData<Unit>()

    fun onFirstPageRewards(): LiveData<Resource<List<Reward>>> = onFirstPageRewards
    fun onNewRewardsPage(): LiveData<Resource<List<Reward>>> = onNewRewardsPage
    fun onEndOfData(): LiveData<Unit> = onEndOfData

    fun fetchFirstPageRewards(deviceId: String) {
        onFirstPageRewards.postValue(Resource.loading())
        viewModelScope.launch {
            usecase.getRewardsTimeline(deviceId, currentPage)
                .map {
                    Timber.d("Got Rewards: ${it.rewards}")
                    hasNextPage = it.hasNextPage
                    currentShownRewards.addAll(it.rewards)
                    onFirstPageRewards.postValue(Resource.success(currentShownRewards))
                }
                .mapLeft {
                    analytics.trackEventFailure(it.code)
                    handleFailure(it)
                }
        }
    }

    fun fetchNewPageRewards(deviceId: String) {
        if (hasNextPage && !blockNewPageRequest) {
            onNewRewardsPage.postValue(Resource.loading())
            viewModelScope.launch {
                currentPage++
                blockNewPageRequest = true

                usecase.getRewardsTimeline(deviceId, currentPage).map {
                    Timber.d("Got Rewards: ${it.rewards}")
                    hasNextPage = it.hasNextPage
                    currentShownRewards.addAll(it.rewards)
                    /**
                     * Add an empty reward at the end in order to let the adapter know that we
                     * reached the end of the data and show the respective card
                     */
                    if (!hasNextPage) {
                        currentShownRewards.add(Reward.empty())
                    }
                    onNewRewardsPage.postValue(Resource.success(currentShownRewards))
                }.onLeft {
                    analytics.trackEventFailure(it.code)
                }

                blockNewPageRequest = false
            }
        } else if (!hasNextPage) {
            // TODO: Empty reward on last page
            onEndOfData.postValue(Unit)
        }
    }

    private fun handleFailure(failure: Failure) {
        onFirstPageRewards.postValue(Resource.error(failure.getDefaultMessage()))
    }
}
