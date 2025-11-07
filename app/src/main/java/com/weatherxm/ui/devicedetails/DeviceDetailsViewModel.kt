package com.weatherxm.ui.devicedetails

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.ApiError
import com.weatherxm.data.models.Failure
import com.weatherxm.ui.common.DeviceRelation
import com.weatherxm.ui.common.Resource
import com.weatherxm.ui.common.SingleLiveEvent
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.usecases.AuthUseCase
import com.weatherxm.usecases.DeviceDetailsUseCase
import com.weatherxm.usecases.FollowUseCase
import com.weatherxm.util.Failure.getDefaultMessage
import com.weatherxm.util.RefreshHandler
import com.weatherxm.util.Resources
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit

@Suppress("LongParameterList", "TooManyFunctions")
class DeviceDetailsViewModel(
    var device: UIDevice = UIDevice.empty(),
    var openExplorerOnBack: Boolean,
    private val useCase: DeviceDetailsUseCase,
    private val authUseCase: AuthUseCase,
    private val followUseCase: FollowUseCase,
    private val resources: Resources,
    private val analytics: AnalyticsWrapper,
    private val dispatcher: CoroutineDispatcher,
) : ViewModel() {
    companion object {
        private const val REFRESH_INTERVAL_SECONDS = 30L
    }

    private val refreshHandler = RefreshHandler(
        refreshIntervalMillis = TimeUnit.SECONDS.toMillis(REFRESH_INTERVAL_SECONDS)
    )

    private val onFollowStatus = MutableLiveData<Resource<Unit>>()
    private var isLoggedIn: Boolean? = null
    private val onDevicePolling = MutableLiveData<UIDevice>()
    private val onUpdatedDevice = MutableLiveData<UIDevice>()
    private val onDeviceFirstFetch = MutableLiveData<UIDevice>()
    private val _onHealthCheckData = SingleLiveEvent<Resource<String>>()

    val shouldShowTerms = mutableStateOf(false)
    val showNotificationsPrompt = mutableStateOf(false)

    fun onDeviceFirstFetch(): LiveData<UIDevice> = onDeviceFirstFetch
    fun onDevicePolling(): LiveData<UIDevice> = onDevicePolling
    fun onUpdatedDevice(): LiveData<UIDevice> = onUpdatedDevice
    fun onFollowStatus(): LiveData<Resource<Unit>> = onFollowStatus
    fun onHealthCheckData(): LiveData<Resource<String>> = _onHealthCheckData

    fun isLoggedIn() = isLoggedIn

    suspend fun deviceAutoRefresh() = refreshHandler.flow().map {
        useCase.getDevice(device).onRight {
            onDeviceAutoRefresh(it)
        }.onLeft {
            analytics.trackEventFailure(it.code)
        }
    }

    private fun onDeviceAutoRefresh(device: UIDevice) {
        Timber.d("Got Device using polling: ${device.name}")
        /**
         * If current device is empty and we got a new one, we need to trigger onDeviceFirstFetch to
         * let other components use it to fetch their data
         */
        if (this.device.isEmpty()) {
            onDeviceFirstFetch.postValue(device)
        }
        this.device = device
        onDevicePolling.postValue(device)
    }

    fun updateDevice(device: UIDevice) {
        this.device = device
        onUpdatedDevice.postValue(device)
    }

    fun followStation() {
        analytics.trackEventUserAction(
            AnalyticsService.ParamValue.DEVICE_DETAILS_FOLLOW.paramValue,
            AnalyticsService.ParamValue.FOLLOW.paramValue
        )
        onFollowStatus.postValue(Resource.loading())
        viewModelScope.launch(dispatcher) {
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
            AnalyticsService.ParamValue.DEVICE_DETAILS_FOLLOW.paramValue,
            AnalyticsService.ParamValue.UNFOLLOW.paramValue
        )
        onFollowStatus.postValue(Resource.loading())
        viewModelScope.launch(dispatcher) {
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

    fun setAcceptTerms() {
        shouldShowTerms.value = false
        useCase.setAcceptTerms()
    }

    fun checkDeviceNotificationsPrompt() {
        showNotificationsPrompt.value = false
        useCase.checkDeviceNotificationsPrompt()
    }

    fun getDeviceHealthCheck() {
        /**
         * While in this screen, if we already have the data, don't refetch it just post it.
         */
        if (!_onHealthCheckData.value?.data.isNullOrEmpty()) {
            _onHealthCheckData.postValue(Resource.success(_onHealthCheckData.value?.data))
            return
        }

        viewModelScope.launch(dispatcher) {
            _onHealthCheckData.postValue(Resource.loading())
            useCase.getDeviceHealthCheck(device.name).apply {
                if (this.isNullOrEmpty()) {
                    _onHealthCheckData.postValue(Resource.error(""))
                } else {
                    _onHealthCheckData.postValue(Resource.success(this))
                }
            }
        }
    }

    init {
        viewModelScope.launch(dispatcher) {
            isLoggedIn = authUseCase.isLoggedIn()
            if (isLoggedIn == true) {
                shouldShowTerms.value = useCase.shouldShowTermsPrompt().also {
                    /**
                     * If we don't need to show the terms dialog, then check if we should show
                     * the notifications prompt on owned devices.
                     */
                    if (!it && device.isOwned()) {
                        showNotificationsPrompt.value = useCase.showDeviceNotificationsPrompt()
                    }
                }
            }
        }
    }
}
