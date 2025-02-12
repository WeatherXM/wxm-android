package com.weatherxm.ui.cellinfo

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.ApiError
import com.weatherxm.data.models.Failure
import com.weatherxm.ui.common.Resource
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.explorer.UICell
import com.weatherxm.usecases.AuthUseCase
import com.weatherxm.usecases.ExplorerUseCase
import com.weatherxm.usecases.FollowUseCase
import com.weatherxm.util.Failure.getDefaultMessage
import com.weatherxm.util.Resources
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

@Suppress("LongParameterList")
class CellInfoViewModel(
    val cell: UICell,
    private val resources: Resources,
    private val explorerUseCase: ExplorerUseCase,
    private val followUseCase: FollowUseCase,
    private val authUseCase: AuthUseCase,
    private val analytics: AnalyticsWrapper,
    private val dispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val onCellDevices = MutableLiveData<Resource<List<UIDevice>>>(Resource.loading())
    private val address = MutableLiveData<String>()
    private var isLoggedIn: Boolean? = null
    private val onFollowStatus = MutableLiveData<Resource<Unit>>()

    fun onCellDevices(): LiveData<Resource<List<UIDevice>>> = onCellDevices
    fun address(): LiveData<String> = address
    fun onFollowStatus(): LiveData<Resource<Unit>> = onFollowStatus

    fun isLoggedIn() = isLoggedIn

    fun fetchDevices() {
        onCellDevices.postValue(Resource.loading())
        viewModelScope.launch(dispatcher) {
            if (cell.index.isEmpty()) {
                Timber.w("Getting cell devices failed: cell index = null")
                onCellDevices.postValue(
                    Resource.error(resources.getString(R.string.error_cell_devices_no_data))
                )
                return@launch
            }

            explorerUseCase.getCellDevices(cell).onRight {
                onCellDevices.postValue(Resource.success(it))
                it.firstOrNull { device ->
                    !device.address.isNullOrEmpty()
                }?.address?.let { address ->
                    this@CellInfoViewModel.address.postValue(address)
                }
            }.onLeft {
                analytics.trackEventFailure(it.code)
                onCellDevices.postValue(
                    Resource.error(resources.getString(R.string.error_cell_devices_no_data))
                )
            }
        }
    }

    fun followStation(deviceId: String) {
        analytics.trackEventUserAction(
            AnalyticsService.ParamValue.EXPLORER_DEVICE_LIST_FOLLOW.paramValue,
            AnalyticsService.ParamValue.FOLLOW.paramValue
        )
        onFollowStatus.postValue(Resource.loading())
        viewModelScope.launch(dispatcher) {
            followUseCase.followStation(deviceId).onRight {
                Timber.d("[Follow Device] Success")
                onFollowStatus.postValue(Resource.success(Unit))
                fetchDevices()
            }.onLeft {
                Timber.e("[Follow Device] Error $it")
                handleFollowError(it)
            }
        }
    }

    fun unFollowStation(deviceId: String) {
        analytics.trackEventUserAction(
            AnalyticsService.ParamValue.EXPLORER_DEVICE_LIST_FOLLOW.paramValue,
            AnalyticsService.ParamValue.UNFOLLOW.paramValue
        )
        onFollowStatus.postValue(Resource.loading())
        viewModelScope.launch(dispatcher) {
            followUseCase.unfollowStation(deviceId).onRight {
                Timber.d("[Unfollow Device] Success")
                onFollowStatus.postValue(Resource.success(Unit))
                fetchDevices()
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
        viewModelScope.launch(dispatcher) {
            isLoggedIn = authUseCase.isLoggedIn()
        }
    }
}
