package com.weatherxm.ui.devicesettings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.data.ApiError
import com.weatherxm.data.BatteryState
import com.weatherxm.data.DeviceProfile
import com.weatherxm.ui.common.DeviceRelation
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.UIError
import com.weatherxm.ui.common.capitalizeWords
import com.weatherxm.ui.common.unmask
import com.weatherxm.usecases.StationSettingsUseCase
import com.weatherxm.util.Analytics
import com.weatherxm.util.ResourcesHelper
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class DeviceSettingsViewModel(var device: UIDevice) : ViewModel(), KoinComponent {
    private val usecase: StationSettingsUseCase by inject()
    private val resHelper: ResourcesHelper by inject()
    private val analytics: Analytics by inject()

    private val onEditNameChange = MutableLiveData<String>()
    private val onDeviceRemoved = MutableLiveData<Boolean>()
    private val onDeviceInfo = MutableLiveData<List<UIDeviceInfo>>()
    private val onError = MutableLiveData<UIError>()
    private val onLoading = MutableLiveData<Boolean>()

    fun onEditNameChange(): LiveData<String> = onEditNameChange
    fun onDeviceRemoved(): LiveData<Boolean> = onDeviceRemoved
    fun onDeviceInfo(): LiveData<List<UIDeviceInfo>> = onDeviceInfo
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
            viewModelScope.launch {
                usecase.setFriendlyName(device.id, friendlyName)
                    .map {
                        analytics.trackEventViewContent(
                            Analytics.ParamValue.CHANGE_STATION_NAME_RESULT.paramValue,
                            Analytics.ParamValue.CHANGE_STATION_NAME_RESULT_ID.paramValue,
                            success = 1L
                        )
                        onEditNameChange.postValue(friendlyName)
                    }
                    .mapLeft {
                        analytics.trackEventFailure(it.code)
                        analytics.trackEventViewContent(
                            Analytics.ParamValue.CHANGE_STATION_NAME_RESULT.paramValue,
                            Analytics.ParamValue.CHANGE_STATION_NAME_RESULT_ID.paramValue,
                            success = 0L
                        )
                        onError.postValue(
                            UIError(resHelper.getString(R.string.error_reach_out_short))
                        )
                    }
                onLoading.postValue(false)
            }
        }
    }

    private fun clearFriendlyName() {
        if (device.friendlyName?.isNotEmpty() == true) {
            onLoading.postValue(true)
            viewModelScope.launch {
                usecase.clearFriendlyName(device.id)
                    .map {
                        analytics.trackEventViewContent(
                            Analytics.ParamValue.CHANGE_STATION_NAME_RESULT.paramValue,
                            Analytics.ParamValue.CHANGE_STATION_NAME_RESULT_ID.paramValue,
                            success = 1L
                        )
                        onEditNameChange.postValue(device.name)
                    }
                    .mapLeft {
                        analytics.trackEventFailure(it.code)
                        analytics.trackEventViewContent(
                            Analytics.ParamValue.CHANGE_STATION_NAME_RESULT.paramValue,
                            Analytics.ParamValue.CHANGE_STATION_NAME_RESULT_ID.paramValue,
                            success = 0L
                        )
                        onError.postValue(
                            UIError(resHelper.getString(R.string.error_reach_out_short))
                        )
                    }
                onLoading.postValue(false)
            }
        }
    }

    fun removeDevice() {
        onLoading.postValue(true)
        viewModelScope.launch {
            device.label?.let { serialNumber ->
                usecase.removeDevice(serialNumber.unmask(), device.id)
                    .map {
                        Timber.d("Device ${device.name} removed.")
                        onDeviceRemoved.postValue(true)
                    }
                    .mapLeft {
                        analytics.trackEventFailure(it.code)
                        Timber.e("Error when trying to remove device: $it")
                        val error = when (it) {
                            is ApiError.UserError.ClaimError.InvalidClaimId -> {
                                resHelper.getString(R.string.error_invalid_device_identifier)
                            }
                            else -> {
                                resHelper.getString(R.string.error_reach_out_short)
                            }
                        }
                        onError.postValue(UIError(error))
                    }
            } ?: onError.postValue(UIError(resHelper.getString(R.string.error_reach_out_short)))
            onLoading.postValue(false)
        }
    }

    fun getDeviceInformation() {
        val deviceInfo = getDeviceInfoFromDevice()
        onLoading.postValue(true)
        viewModelScope.launch {
            usecase.getDeviceInfo(device.id).onLeft {
                analytics.trackEventFailure(it.code)
                Timber.d("$it: Fetching remote device info failed for device: $device")
                onDeviceInfo.postValue(deviceInfo)
            }.onRight { infoFromAPI ->
                Timber.d("Got device info: $deviceInfo")
                infoFromAPI.weatherStation?.batteryState?.let {
                    if (it == BatteryState.low) {
                        deviceInfo.add(
                            2,
                            UIDeviceInfo(
                                resHelper.getString(R.string.battery_level),
                                resHelper.getString(R.string.battery_level_low),
                                warning = resHelper.getString(R.string.battery_level_low_message)
                            )
                        )
                    } else {
                        deviceInfo.add(
                            2,
                            UIDeviceInfo(
                                resHelper.getString(R.string.battery_level),
                                resHelper.getString(R.string.battery_level_ok)
                            )
                        )
                    }
                }

                infoFromAPI.weatherStation?.hwVersion?.let {
                    deviceInfo.add(UIDeviceInfo(resHelper.getString(R.string.hardware_version), it))
                }
                infoFromAPI.weatherStation?.lastHotspot?.let {
                    deviceInfo.add(
                        UIDeviceInfo(
                            resHelper.getString(R.string.last_hotspot),
                            it.replace("-", " ").capitalizeWords()
                        )
                    )
                }
                infoFromAPI.weatherStation?.lastTxRssi?.let {
                    deviceInfo.add(
                        UIDeviceInfo(
                            resHelper.getString(R.string.last_tx_rssi),
                            resHelper.getString(R.string.rssi, it)
                        )
                    )
                }
                infoFromAPI.gateway?.gpsSats?.let {
                    deviceInfo.add(UIDeviceInfo(resHelper.getString(R.string.gps_number_sats), it))
                }
                infoFromAPI.gateway?.wifiRssi?.let {
                    deviceInfo.add(
                        UIDeviceInfo(
                            resHelper.getString(R.string.wifi_rssi),
                            resHelper.getString(R.string.rssi, it)
                        )
                    )
                }
                onDeviceInfo.postValue(deviceInfo)
            }
            onLoading.postValue(false)
        }
    }

    private fun getDeviceInfoFromDevice(): MutableList<UIDeviceInfo> {
        return mutableListOf<UIDeviceInfo>().apply {
            add(UIDeviceInfo(resHelper.getString(R.string.station_default_name), device.name))
            device.claimedAt?.let {
                add(
                    UIDeviceInfo(
                        resHelper.getString(R.string.claimed_at),
                        it.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))
                    )
                )
            }
            if (device.profile == DeviceProfile.Helium) {
                device.label?.unmask()?.let {
                    add(UIDeviceInfo(resHelper.getString(R.string.dev_eui), it))
                }
            } else {
                device.label?.unmask()?.let {
                    add(UIDeviceInfo(resHelper.getString(R.string.device_serial_number_title), it))
                }
            }
            device.currentFirmware?.let { current ->
                if (device.needsUpdate() && device.relation == DeviceRelation.OWNED
                    && usecase.shouldShowOTAPrompt(device)
                ) {
                    add(
                        UIDeviceInfo(
                            resHelper.getString(R.string.firmware_version),
                            "$current ➞ ${device.assignedFirmware}",
                            UIDeviceAction(
                                resHelper.getString(R.string.action_update_firmware),
                                ActionType.UPDATE_FIRMWARE
                            )
                        )
                    )
                } else {
                    add(UIDeviceInfo(resHelper.getString(R.string.firmware_version), current))
                }
            }
        }
    }

    fun parseDeviceInfoToShare(deviceInfo: List<UIDeviceInfo>): String {
        var sharingText = ""
        deviceInfo.forEach {
            sharingText += "${it}\n"
        }
        return sharingText
    }
}
