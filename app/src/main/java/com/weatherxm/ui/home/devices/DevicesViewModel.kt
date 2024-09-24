package com.weatherxm.ui.home.devices

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.ApiError
import com.weatherxm.ui.common.Resource
import com.weatherxm.ui.common.DeviceTotalRewards
import com.weatherxm.ui.common.DevicesFilterType
import com.weatherxm.ui.common.DevicesGroupBy
import com.weatherxm.ui.common.DevicesRewards
import com.weatherxm.ui.common.DevicesSortFilterOptions
import com.weatherxm.ui.common.DevicesSortOrder
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.usecases.DeviceListUseCase
import com.weatherxm.usecases.FollowUseCase
import com.weatherxm.util.Failure.getDefaultMessage
import com.weatherxm.util.Resources
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class DevicesViewModel(
    private val deviceListUseCase: DeviceListUseCase,
    private val followUseCase: FollowUseCase,
    private val analytics: AnalyticsWrapper,
    private val resources: Resources
) : ViewModel() {
    private val devices = MutableLiveData<Resource<List<UIDevice>>>().apply {
        value = Resource.loading()
    }
    private val onFollowStatus = MutableLiveData<Resource<Unit>>()
    private val onDevicesRewards = MutableLiveData<DevicesRewards>()

    // Needed for passing info to the activity to show/hide elements when scrolling on the list
    private val showOverlayViews = MutableLiveData(true)

    fun devices(): LiveData<Resource<List<UIDevice>>> = devices
    fun onDevicesRewards(): LiveData<DevicesRewards> = onDevicesRewards
    fun onFollowStatus(): LiveData<Resource<Unit>> = onFollowStatus
    fun showOverlayViews() = showOverlayViews

    fun fetch() {
        this@DevicesViewModel.devices.postValue(Resource.loading())
        viewModelScope.launch(Dispatchers.IO) {
            deviceListUseCase.getUserDevices()
                .map { devices ->
                    Timber.d("Got ${devices.size} devices")
                    this@DevicesViewModel.devices.postValue(Resource.success(devices))
                    calculateRewards(devices)
                }
                .mapLeft {
                    analytics.trackEventFailure(it.code)
                    this@DevicesViewModel.devices.postValue(Resource.error(it.getDefaultMessage()))
                }
        }
    }

    private fun calculateRewards(devices: List<UIDevice>) {
        var totalRewards = 0F
        var latestRewards = 0F
        val devicesWithRewards = mutableListOf<DeviceTotalRewards>()
        devices.forEach {
            if (it.isOwned()) {
                totalRewards += it.totalRewards ?: 0F
                latestRewards += it.actualReward ?: 0F
                devicesWithRewards.add(
                    DeviceTotalRewards(it.id, it.getDefaultOrFriendlyName(), it.totalRewards)
                )
            }
        }
        onDevicesRewards.postValue(DevicesRewards(totalRewards, latestRewards, devicesWithRewards))
    }

    fun onScroll(dy: Int) {
        showOverlayViews.postValue(dy <= 0)
    }

    fun unFollowStation(deviceId: String) {
        onFollowStatus.postValue(Resource.loading())
        viewModelScope.launch {
            followUseCase.unfollowStation(deviceId).onRight {
                Timber.d("[Unfollow Device] Success")
                onFollowStatus.postValue(Resource.success(Unit))
                fetch()
            }.onLeft {
                Timber.e("[Unfollow Device] Error $it")
                analytics.trackEventFailure(it.code)
                val errorMessage = when (it) {
                    is ApiError.DeviceNotFound -> {
                        resources.getString(R.string.error_device_not_found)
                    }
                    is ApiError.MaxFollowed -> {
                        resources.getString(R.string.error_max_followed)
                    }
                    is ApiError.GenericError.JWTError.UnauthorizedError -> it.message
                    else -> it.getDefaultMessage(R.string.error_reach_out_short)
                } ?: resources.getString(R.string.error_reach_out_short)
                onFollowStatus.postValue(Resource.error(errorMessage))
            }
        }
    }

    fun setDevicesSortFilterOptions(
        sortOrderId: Int,
        filterId: Int,
        groupById: Int
    ) {
        val sortOrder = when (sortOrderId) {
            R.id.dateAdded -> DevicesSortOrder.DATE_ADDED
            R.id.name -> DevicesSortOrder.NAME
            else -> DevicesSortOrder.LAST_ACTIVE
        }

        val filterType = when (filterId) {
            R.id.showAll -> DevicesFilterType.ALL
            R.id.ownedOnly -> DevicesFilterType.OWNED
            else -> DevicesFilterType.FAVORITES
        }

        val groupBy = when (groupById) {
            R.id.noGrouping -> DevicesGroupBy.NO_GROUPING
            R.id.relationship -> DevicesGroupBy.RELATIONSHIP
            else -> DevicesGroupBy.STATUS
        }

        deviceListUseCase.setDevicesSortFilterOptions(
            DevicesSortFilterOptions(sortOrder, filterType, groupBy)
        )
    }

    fun getDevicesSortFilterOptions(): DevicesSortFilterOptions {
        return deviceListUseCase.getDevicesSortFilterOptions()
    }
}
