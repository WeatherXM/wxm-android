package com.weatherxm.ui.rewardslist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.data.Failure
import com.weatherxm.data.Resource
import com.weatherxm.ui.common.UIRewardObject
import com.weatherxm.usecases.RewardsUseCase
import com.weatherxm.util.Analytics
import com.weatherxm.util.UIErrors.getDefaultMessage
import com.weatherxm.util.toISODate
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.time.ZonedDateTime

class RewardsListViewModel : ViewModel(), KoinComponent {
    companion object {
        const val FETCH_INTERVAL_MONTHS = 3L
    }

    private val usecase: RewardsUseCase by inject()
    private val analytics: Analytics by inject()

    private var currentPage = 0
    private var hasNextPage = false
    private var blockNewPageRequest = false
    private var reachedTotal = false
    private var currFromDate = ZonedDateTime.now().minusMonths(FETCH_INTERVAL_MONTHS)
    private var currToDate = ZonedDateTime.now()
    private val currentShownRewards = mutableListOf<UIRewardObject>()

    private val onFirstPageRewards = MutableLiveData<Resource<List<UIRewardObject>>>().apply {
        value = Resource.loading()
    }

    private val onNewRewardsPage = MutableLiveData<Resource<List<UIRewardObject>>>()

    fun onFirstPageRewards(): LiveData<Resource<List<UIRewardObject>>> = onFirstPageRewards

    fun onNewRewardsPage(): LiveData<Resource<List<UIRewardObject>>> = onNewRewardsPage

    fun fetchFirstPageRewards(deviceId: String) {
        onFirstPageRewards.postValue(Resource.loading())
        viewModelScope.launch {
            usecase.getTransactions(deviceId, currentPage, currFromDate.toISODate())
                .map {
                    Timber.d("Got Rewards: ${it.rewards}")
                    hasNextPage = it.hasNextPage
                    reachedTotal = it.reachedTotal
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

                usecase.getTransactions(
                    deviceId,
                    currentPage,
                    currFromDate.toISODate(),
                    currToDate.toISODate()
                ).map {
                    Timber.d("Got Rewards: ${it.rewards}")
                    hasNextPage = it.hasNextPage
                    reachedTotal = it.reachedTotal
                    /*
                     * There is an edge case where rewards are empty, we have next page
                     * and we need to hide the spinner (it's literally a success) in the UI,
                     * but to not refresh the adapter. So we pass null on these occasions.
                     */
                    if (it.rewards.isNotEmpty()) {
                        currentShownRewards.addAll(it.rewards)
                        onNewRewardsPage.postValue(Resource.success(currentShownRewards))
                    } else {
                        onNewRewardsPage.postValue(Resource.success(null))
                    }
                }.onLeft {
                    analytics.trackEventFailure(it.code)
                }

                blockNewPageRequest = false
            }
        } else if (!hasNextPage && !blockNewPageRequest && !reachedTotal) {
            onNewRewardsPage.postValue(Resource.loading())
            viewModelScope.launch {
                currentPage = 0
                blockNewPageRequest = true
                currToDate = currFromDate
                currFromDate = currFromDate.minusMonths(FETCH_INTERVAL_MONTHS)

                usecase.getTransactions(
                    deviceId,
                    currentPage,
                    currFromDate.toISODate(),
                    currToDate.toISODate()
                ).map {
                    Timber.d("Got Rewards: ${it.rewards}")
                    hasNextPage = it.hasNextPage
                    reachedTotal = it.reachedTotal
                    currentShownRewards.addAll(it.rewards)
                    /*
                     * There is an edge case where rewards are empty, we have next page
                     * and we need to hide the spinner (it's literally a success) in the UI,
                     * but to not refresh the adapter. So we pass null on these occasions.
                     */
                    if (it.rewards.isNotEmpty()) {
                        currentShownRewards.addAll(it.rewards)
                        onNewRewardsPage.postValue(Resource.success(currentShownRewards))
                    } else {
                        onNewRewardsPage.postValue(Resource.success(null))
                    }
                }.onLeft {
                    analytics.trackEventFailure(it.code)
                }
                blockNewPageRequest = false
            }
        }
    }

    private fun handleFailure(failure: Failure) {
        onFirstPageRewards.postValue(Resource.error(failure.getDefaultMessage()))
    }
}
