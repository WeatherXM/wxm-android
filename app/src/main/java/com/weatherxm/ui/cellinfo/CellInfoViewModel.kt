package com.weatherxm.ui.cellinfo

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.getOrElse
import com.weatherxm.R
import com.weatherxm.data.ApiError
import com.weatherxm.data.Failure
import com.weatherxm.data.Resource
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.explorer.UICell
import com.weatherxm.usecases.AuthUseCase
import com.weatherxm.usecases.ExplorerUseCase
import com.weatherxm.usecases.FollowUseCase
import com.weatherxm.util.Analytics
import com.weatherxm.util.ResourcesHelper
import com.weatherxm.util.UIErrors.getDefaultMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class CellInfoViewModel(val cell: UICell) : ViewModel(), KoinComponent {
    private val resHelper: ResourcesHelper by inject()
    private val explorerUseCase: ExplorerUseCase by inject()
    private val followUseCase: FollowUseCase by inject()
    private val authUseCase: AuthUseCase by inject()
    private val analytics: Analytics by inject()

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
        viewModelScope.launch(Dispatchers.IO) {
            if (cell.index.isEmpty()) {
                Timber.w("Getting cell devices failed: cell index = null")
                onCellDevices.postValue(
                    Resource.error(resHelper.getString(R.string.error_cell_devices_no_data))
                )
                return@launch
            }

            explorerUseCase.getCellDevices(cell)
                .onRight {
                    onCellDevices.postValue(Resource.success(it))
                    it.firstOrNull { device ->
                        !device.address.isNullOrEmpty()
                    }?.address?.let { address ->
                        this@CellInfoViewModel.address.postValue(address)
                    }
                }
                .onLeft {
                    analytics.trackEventFailure(it.code)
                    onCellDevices.postValue(
                        Resource.error(resHelper.getString(R.string.error_cell_devices_no_data))
                    )
                }
        }
    }

    fun followStation(deviceId: String) {
        onFollowStatus.postValue(Resource.loading())
        viewModelScope.launch {
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
        onFollowStatus.postValue(Resource.loading())
        viewModelScope.launch {
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
            is ApiError.DeviceNotFound -> resHelper.getString(R.string.error_device_not_found)
            is ApiError.MaxFollowed -> resHelper.getString(R.string.error_max_followed)
            is ApiError.GenericError.JWTError.UnauthorizedError -> failure.message
            else -> failure.getDefaultMessage(R.string.error_reach_out_short)
        } ?: resHelper.getString(R.string.error_reach_out_short)
        onFollowStatus.postValue(Resource.error(errorMessage))
    }

    init {
        viewModelScope.launch {
            isLoggedIn = authUseCase.isLoggedIn().getOrElse { false }
        }
    }
}
