package com.weatherxm.ui.devicedetails

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.getOrElse
import com.weatherxm.R
import com.weatherxm.analytics.Analytics
import com.weatherxm.data.ApiError
import com.weatherxm.data.Failure
import com.weatherxm.data.Resource
import com.weatherxm.ui.common.DeviceRelation
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.explorer.UICell
import com.weatherxm.usecases.AuthUseCase
import com.weatherxm.usecases.DeviceDetailsUseCase
import com.weatherxm.usecases.FollowUseCase
import com.weatherxm.analytics.AnalyticsImpl
import com.weatherxm.util.Failure.getDefaultMessage
import com.weatherxm.util.RefreshHandler
import com.weatherxm.util.Resources
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit

@Suppress("LongParameterList")
class DeviceDetailsViewModel(
    var device: UIDevice = UIDevice.empty(),
    var openExplorerOnBack: Boolean,
    private val deviceDetailsUseCase: DeviceDetailsUseCase,
    private val authUseCase: AuthUseCase,
    private val resources: Resources,
    private val followUseCase: FollowUseCase,
    private val analytics: AnalyticsImpl
) : ViewModel() {
    companion object {
        private const val REFRESH_INTERVAL_SECONDS = 30L
    }

    private val refreshHandler = RefreshHandler(
        refreshIntervalMillis = TimeUnit.SECONDS.toMillis(REFRESH_INTERVAL_SECONDS)
    )

    private val address = MutableLiveData<String?>()
    private val onFollowStatus = MutableLiveData<Resource<Unit>>()
    private var isLoggedIn: Boolean? = null
    private val onDevicePolling = MutableLiveData<UIDevice>()
    private val onUpdatedDevice = MutableLiveData<UIDevice>()
    private val onDeviceFirstFetch = MutableLiveData<UIDevice>()

    fun onDeviceFirstFetch(): LiveData<UIDevice> = onDeviceFirstFetch
    fun onDevicePolling(): LiveData<UIDevice> = onDevicePolling
    fun onUpdatedDevice(): LiveData<UIDevice> = onUpdatedDevice
    fun onFollowStatus(): LiveData<Resource<Unit>> = onFollowStatus
    fun address(): LiveData<String?> = address

    fun isLoggedIn() = isLoggedIn

    suspend fun deviceAutoRefresh() = refreshHandler.flow()
        .map {
            deviceDetailsUseCase.getUserDevice(device)
                .onRight {
                    Timber.d("Got Device using polling: ${it.name}")
                    if (device.isDeviceFromSearchResult) {
                        onDeviceFirstFetch.postValue(it)
                    }
                    this.device = it
                    onDevicePolling.postValue(it)
                }
                .onLeft {
                    analytics.trackEventFailure(it.code)
                }
        }

    fun updateDevice(device: UIDevice) {
        this.device = device
        onUpdatedDevice.postValue(device)
    }

    fun fetchAddressFromCell() {
        if (!device.address.isNullOrEmpty()) {
            address.postValue(device.address)
            return
        }
        viewModelScope.launch {
            device.cellCenter?.let {
                address.postValue(
                    deviceDetailsUseCase.getAddressOfCell(UICell(device.cellIndex, it))
                )
            }
        }
    }

    fun createNormalizedName(): String {
        return if (!device.isEmpty()) {
            device.toNormalizedName()
        } else {
            String.empty()
        }
    }

    fun followStation() {
        analytics.trackEventUserAction(
            Analytics.ParamValue.DEVICE_DETAILS_FOLLOW.paramValue,
            Analytics.ParamValue.FOLLOW.paramValue
        )
        onFollowStatus.postValue(Resource.loading())
        viewModelScope.launch {
            followUseCase.followStation(device.id).onRight {
                Timber.d("[Follow Device] Success")
                device.relation = DeviceRelation.FOLLOWED
                onFollowStatus.postValue(Resource.success(Unit))
            }.onLeft {
                Timber.e("[Follow Device] Error $it")
                handleFollowError(it)
            }
        }
    }

    fun unFollowStation() {
        analytics.trackEventUserAction(
            Analytics.ParamValue.DEVICE_DETAILS_FOLLOW.paramValue,
            Analytics.ParamValue.UNFOLLOW.paramValue
        )
        onFollowStatus.postValue(Resource.loading())
        viewModelScope.launch {
            followUseCase.unfollowStation(device.id).onRight {
                Timber.d("[Unfollow Device] Success")
                device.relation = DeviceRelation.UNFOLLOWED
                onFollowStatus.postValue(Resource.success(Unit))
            }.onLeft {
                Timber.e("[Unfollow Device] Error $it")
                handleFollowError(it)
            }
        }
    }

    private fun handleFollowError(failure: Failure) {
        analytics.trackEventFailure(failure.code)
        val errorMessage = when (failure) {
            is ApiError.DeviceNotFound -> resources.getString(R.string.error_device_not_found)
            is ApiError.MaxFollowed -> resources.getString(R.string.error_max_followed)
            is ApiError.GenericError.JWTError.UnauthorizedError -> failure.message
            else -> failure.getDefaultMessage(R.string.error_reach_out_short)
        } ?: resources.getString(R.string.error_reach_out_short)
        onFollowStatus.postValue(Resource.error(errorMessage))
    }

    init {
        viewModelScope.launch {
            isLoggedIn = authUseCase.isLoggedIn().getOrElse { false }
        }
    }
}
