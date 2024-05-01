package com.weatherxm.ui.explorer.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.data.Resource
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.explorer.SearchResult
import com.weatherxm.usecases.ExplorerUseCase
import com.weatherxm.analytics.AnalyticsImpl
import com.weatherxm.util.Failure.getDefaultMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

class NetworkSearchViewModel(
    private val explorerUseCase: ExplorerUseCase,
    private val analytics: AnalyticsImpl
) : ViewModel() {
    companion object {
        const val NETWORK_SEARCH_REQUEST_THRESHOLD = 1000L
    }

    private val onSearchResults = MutableLiveData<Resource<List<SearchResult>>>()
    fun onSearchResults(): LiveData<Resource<List<SearchResult>>> = onSearchResults

    private val onRecentSearches = MutableLiveData<List<SearchResult>>()
    fun onRecentSearches(): LiveData<List<SearchResult>> = onRecentSearches

    private var lastSearchedQuery = String.empty()
    private var query = String.empty()
    private var networkSearchJob: Job? = null

    fun getQuery() = query

    fun setQuery(query: String) {
        this.query = query
    }

    fun cancelNetworkSearchJob() {
        networkSearchJob?.cancel("Cancelling running network search job.")
        onSearchResults.value?.data?.let {
            onSearchResults.postValue(Resource.success(it))
        }
    }

    fun networkSearch(runImmediately: Boolean = false) {
        if (query.trim() == lastSearchedQuery.trim()) {
            return
        }

        onSearchResults.postValue(Resource.loading())

        networkSearchJob?.let {
            if (it.isActive) {
                it.cancel("Cancelling running network search job.")
            }
        }

        networkSearchJob = viewModelScope.launch(Dispatchers.IO) {
            // Wait for threshold time to pass before sending an API request
            if (!runImmediately) {
                delay(NETWORK_SEARCH_REQUEST_THRESHOLD)
            }
            lastSearchedQuery = query
            explorerUseCase.networkSearch(query)
                .map {
                    onSearchResults.postValue(Resource.success(it))
                }
                .mapLeft {
                    analytics.trackEventFailure(it.code)
                    onSearchResults.postValue(Resource.error(it.getDefaultMessage()))
                }
        }

        networkSearchJob?.invokeOnCompletion {
            if (it is CancellationException) {
                Timber.d("Cancelled running network search job.")
            }
        }
    }

    fun getRecentSearches() {
        viewModelScope.launch(Dispatchers.IO) {
            explorerUseCase.getRecentSearches().apply {
                onRecentSearches.postValue(this)
            }
        }
    }

    fun onSearchClicked(searchResult: SearchResult) {
        viewModelScope.launch(Dispatchers.IO) {
            explorerUseCase.setRecentSearch(searchResult)
            query = String.empty()
        }
    }
}
