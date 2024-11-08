package com.weatherxm.ui.networkstats

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.ui.common.Resource
import com.weatherxm.usecases.StatsUseCase
import com.weatherxm.util.Failure.getDefaultMessage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class NetworkStatsViewModel(
    private val usecase: StatsUseCase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val onNetworkStats = MutableLiveData<Resource<NetworkStats>>()

    fun onNetworkStats() = onNetworkStats

    fun getNetworkStats() {
        onNetworkStats.postValue(Resource.loading())
        viewModelScope.launch(dispatcher) {
            usecase.getNetworkStats()
                .onRight {
                    onNetworkStats.postValue(Resource.success(it))
                }
                .onLeft {
                    Timber.d("Failed getting network stats: $it")
                    onNetworkStats.postValue(Resource.error(it.getDefaultMessage()))
                }
        }
    }
}
