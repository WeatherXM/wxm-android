package com.weatherxm.ui.networkstats

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.data.Resource
import com.weatherxm.ui.common.MainnetInfo
import com.weatherxm.usecases.StatsUseCase
import com.weatherxm.util.Failure.getDefaultMessage
import kotlinx.coroutines.launch
import timber.log.Timber

class NetworkStatsViewModel(private val usecase: StatsUseCase) : ViewModel() {

    private val onMainnet = MutableLiveData<MainnetInfo>()
    private val onNetworkStats = MutableLiveData<Resource<NetworkStats>>()

    fun onMainnet(): LiveData<MainnetInfo> = onMainnet
    fun onNetworkStats() = onNetworkStats

    fun fetchMainnetStatus() {
        viewModelScope.launch {
            if (usecase.isMainnetEnabled()) {
                onMainnet.postValue(usecase.getMainnetInfo())
            }
        }
    }

    fun getNetworkStats() {
        onNetworkStats.postValue(Resource.loading())
        viewModelScope.launch {
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
