package com.weatherxm.ui.devicesettings

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.ApiError
import com.weatherxm.data.models.BatteryState
import com.weatherxm.data.models.DeviceInfo
import com.weatherxm.ui.common.DeviceAlert
import com.weatherxm.ui.common.DeviceAlertType
import com.weatherxm.ui.common.RewardSplitsData
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.UIError
import com.weatherxm.ui.common.unmask
import com.weatherxm.usecases.StationSettingsUseCase
import com.weatherxm.util.Resources
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import timber.log.Timber

abstract class BaseDeviceSettingsViewModel(
    var device: UIDevice,
    private val usecase: StationSettingsUseCase,
    private val resources: Resources,
    private val analytics: AnalyticsWrapper,
    protected val dispatcher: CoroutineDispatcher
) : ViewModel() {
    private val onEditNameChange = MutableLiveData<String>()
    private val onDeviceRemoved = MutableLiveData<Boolean>()
    private val onError = MutableLiveData<UIError>()
    protected val onLoading = MutableLiveData<Boolean>()

    fun onEditNameChange(): LiveData<String> = onEditNameChange
    fun onDeviceRemoved(): LiveData<Boolean> = onDeviceRemoved
    fun onError(): LiveData<UIError> = onError
    fun onLoading(): LiveData<Boolean> = onLoading

    fun setOrClearFriendlyName(friendlyName: String?) {
        if (friendlyName == null) {
            clearFriendlyName()
        } else {
            setFriendlyName(friendlyName)
        }
    }

    private fun setFriendlyName(friendlyName: String) {
        if (friendlyName.isNotEmpty() && friendlyName != device.friendlyName) {
            onLoading.postValue(true)
            viewModelScope.launch(dispatcher) {
                usecase.setFriendlyName(device.id, friendlyName).onRight {
                    analytics.trackEventViewContent(
                        AnalyticsService.ParamValue.CHANGE_STATION_NAME_RESULT.paramValue,
                        AnalyticsService.ParamValue.CHANGE_STATION_NAME_RESULT_ID.paramValue,
                        success = 1L
                    )
                    device.friendlyName = friendlyName
                    onEditNameChange.postValue(friendlyName)
                }.onLeft {
                    analytics.trackEventFailure(it.code)
                    analytics.trackEventViewContent(
                        AnalyticsService.ParamValue.CHANGE_STATION_NAME_RESULT.paramValue,
                        AnalyticsService.ParamValue.CHANGE_STATION_NAME_RESULT_ID.paramValue,
                        success = 0L
                    )
                    onError.postValue(
                        UIError(resources.getString(R.string.error_reach_out_short))
                    )
                }
                onLoading.postValue(false)
            }
        }
    }

    private fun clearFriendlyName() {
        if (device.friendlyName?.isNotEmpty() == true) {
            onLoading.postValue(true)
            viewModelScope.launch(dispatcher) {
                usecase.clearFriendlyName(device.id).onRight {
                    analytics.trackEventViewContent(
                        AnalyticsService.ParamValue.CHANGE_STATION_NAME_RESULT.paramValue,
                        AnalyticsService.ParamValue.CHANGE_STATION_NAME_RESULT_ID.paramValue,
                        success = 1L
                    )
                    device.friendlyName = null
                    onEditNameChange.postValue(device.name)
                }.onLeft {
                    analytics.trackEventFailure(it.code)
                    analytics.trackEventViewContent(
                        AnalyticsService.ParamValue.CHANGE_STATION_NAME_RESULT.paramValue,
                        AnalyticsService.ParamValue.CHANGE_STATION_NAME_RESULT_ID.paramValue,
                        success = 0L
                    )
                    onError.postValue(
                        UIError(resources.getString(R.string.error_reach_out_short))
                    )
                }
                onLoading.postValue(false)
            }
        }
    }

    fun removeDevice() {
        onLoading.postValue(true)
        viewModelScope.launch(dispatcher) {
            device.label?.let { serialNumber ->
                usecase.removeDevice(serialNumber.unmask(), device.id).onRight {
                    Timber.d("Device ${device.name} removed.")
                    onDeviceRemoved.postValue(true)
                }.onLeft {
                    analytics.trackEventFailure(it.code)
                    Timber.e("Error when trying to remove device: $it")
                    val error = if (it is ApiError.UserError.ClaimError.InvalidClaimId) {
                        resources.getString(R.string.error_invalid_device_identifier)
                    } else {
                        resources.getString(R.string.error_reach_out_short)
                    }
                    onError.postValue(UIError(error))
                }
            } ?: onError.postValue(UIError(resources.getString(R.string.error_reach_out_short)))
            onLoading.postValue(false)
        }
    }

    fun parseDeviceInfoToShare(deviceInfo: UIDeviceInfo): String {
        return deviceInfo.default.joinToString(separator = "\n") {
            it.toFormattedString()
        }.plus(deviceInfo.gateway.joinToString(separator = "\n", prefix = "\n") {
            it.toFormattedString()
        }).plus(deviceInfo.station.joinToString(separator = "\n", prefix = "\n") {
            it.toFormattedString()
        })
    }

    fun isStakeholder(rewardSplitsData: RewardSplitsData): Boolean {
        return rewardSplitsData.splits.firstOrNull { split ->
            rewardSplitsData.wallet == split.wallet
        } != null
    }

    protected fun handleLowBatteryInfo(
        target: MutableList<UIDeviceInfoItem>,
        batteryState: BatteryState
    ) {
        if (batteryState == BatteryState.low) {
            target.add(
                UIDeviceInfoItem(
                    resources.getString(R.string.battery_level),
                    resources.getString(R.string.battery_level_low),
                    deviceAlert = DeviceAlert.createWarning(DeviceAlertType.LOW_BATTERY)
                )
            )
        } else {
            target.add(
                UIDeviceInfoItem(
                    resources.getString(R.string.battery_level),
                    resources.getString(R.string.battery_level_ok)
                )
            )
        }
    }

    abstract fun getDeviceInformation(context: Context)
    abstract suspend fun handleInfo(context: Context, info: DeviceInfo)
}
