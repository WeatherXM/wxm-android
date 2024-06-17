package com.weatherxm.ui.devicesettings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsService
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.ApiError
import com.weatherxm.data.BatteryState
import com.weatherxm.data.DeviceInfo
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.UIError
import com.weatherxm.ui.common.capitalizeWords
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.common.unmask
import com.weatherxm.usecases.StationSettingsUseCase
import com.weatherxm.util.Resources
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class DeviceSettingsViewModel(
    var device: UIDevice,
    private val usecase: StationSettingsUseCase,
    private val resources: Resources,
    private val analytics: AnalyticsWrapper
) : ViewModel() {
    private val onEditNameChange = MutableLiveData<String>()
    private val onDeviceRemoved = MutableLiveData<Boolean>()
    private val onDeviceInfo = MutableLiveData<UIDeviceInfo>()
    private val onError = MutableLiveData<UIError>()
    private val onLoading = MutableLiveData<Boolean>()

    private val deviceInfoData = UIDeviceInfo(mutableListOf(), mutableListOf(), mutableListOf())

    fun onEditNameChange(): LiveData<String> = onEditNameChange
    fun onDeviceRemoved(): LiveData<Boolean> = onDeviceRemoved
    fun onDeviceInfo(): LiveData<UIDeviceInfo> = onDeviceInfo
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
                            AnalyticsService.ParamValue.CHANGE_STATION_NAME_RESULT.paramValue,
                            AnalyticsService.ParamValue.CHANGE_STATION_NAME_RESULT_ID.paramValue,
                            success = 1L
                        )
                        device.friendlyName = friendlyName
                        onEditNameChange.postValue(friendlyName)
                    }
                    .mapLeft {
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
            viewModelScope.launch {
                usecase.clearFriendlyName(device.id)
                    .map {
                        analytics.trackEventViewContent(
                            AnalyticsService.ParamValue.CHANGE_STATION_NAME_RESULT.paramValue,
                            AnalyticsService.ParamValue.CHANGE_STATION_NAME_RESULT_ID.paramValue,
                            success = 1L
                        )
                        device.friendlyName = null
                        onEditNameChange.postValue(device.name)
                    }
                    .mapLeft {
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

    fun getDeviceInformation() {
        deviceInfoData.default.add(
            UIDeviceInfoItem(resources.getString(R.string.station_default_name), device.name)
        )

        device.bundleTitle?.let {
            deviceInfoData.default.add(
                UIDeviceInfoItem(resources.getString(R.string.bundle_identifier), it)
            )
        }
        device.claimedAt?.let {
            deviceInfoData.default.add(
                UIDeviceInfoItem(
                    resources.getString(R.string.claimed_at),
                    it.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))
                )
            )
        }
        onLoading.postValue(true)
        viewModelScope.launch {
            usecase.getDeviceInfo(device.id).onLeft {
                analytics.trackEventFailure(it.code)
                Timber.d("$it: Fetching remote device info failed for device: $device")
                onDeviceInfo.postValue(deviceInfoData)
            }.onRight { info ->
                Timber.d("Got device info: $info")
                if (device.isHelium()) {
                    handleInfoNoCategories(info)
                } else {
                    handleInfoWithCategories(info)
                }
                onDeviceInfo.postValue(deviceInfoData)
            }
            onLoading.postValue(false)
        }
    }

    private fun handleInfoNoCategories(info: DeviceInfo) {
        info.weatherStation?.model?.let {
            deviceInfoData.default.add(
                UIDeviceInfoItem(resources.getString(R.string.model), it)
            )
        }

        info.weatherStation?.batteryState?.let {
            if (it == BatteryState.low) {
                deviceInfoData.default.add(
                    UIDeviceInfoItem(
                        resources.getString(R.string.battery_level),
                        resources.getString(R.string.battery_level_low),
                        warning = resources.getString(R.string.battery_level_low_message)
                    )
                )
            } else {
                deviceInfoData.default.add(
                    UIDeviceInfoItem(
                        resources.getString(R.string.battery_level),
                        resources.getString(R.string.battery_level_ok)
                    )
                )
            }
        }

        info.weatherStation?.devEUI?.let {
            deviceInfoData.default.add(
                UIDeviceInfoItem(resources.getString(R.string.dev_eui), it)
            )
        }

        device.currentFirmware?.let { current ->
            if (usecase.userShouldNotifiedOfOTA(device) && device.shouldPromptUpdate()) {
                deviceInfoData.default.add(
                    UIDeviceInfoItem(
                        resources.getString(R.string.firmware_version),
                        "$current ➞ ${device.assignedFirmware}",
                        UIDeviceAction(
                            resources.getString(R.string.action_update_firmware),
                            ActionType.UPDATE_FIRMWARE
                        )
                    )
                )
            } else {
                val currentFirmware = if (device.currentFirmware.equals(device.assignedFirmware)) {
                    current
                } else {
                    "$current ${resources.getString(R.string.latest_hint)}"
                }
                deviceInfoData.default.add(
                    UIDeviceInfoItem(
                        resources.getString(R.string.firmware_version), currentFirmware
                    )
                )
            }
        }

        info.weatherStation?.hwVersion?.let {
            deviceInfoData.default.add(
                UIDeviceInfoItem(resources.getString(R.string.hardware_version), it)
            )
        }

        info.weatherStation?.lastHotspot?.let {
            deviceInfoData.default.add(
                UIDeviceInfoItem(
                    resources.getString(R.string.last_hotspot),
                    it.replace("-", " ").capitalizeWords()
                )
            )
        }
        info.weatherStation?.lastTxRssi?.let {
            deviceInfoData.default.add(
                UIDeviceInfoItem(
                    resources.getString(R.string.last_tx_rssi),
                    resources.getString(R.string.rssi, it)
                )
            )
        }

        info.weatherStation?.lastActivity?.let {
            deviceInfoData.default.add(
                UIDeviceInfoItem(
                    resources.getString(R.string.last_weather_station_activity),
                    it.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))
                )
            )
        }
    }

    private fun handleInfoWithCategories(info: DeviceInfo) {
        info.weatherStation?.model?.let {
            deviceInfoData.station.add(
                UIDeviceInfoItem(resources.getString(R.string.model), it)
            )
        }

        info.weatherStation?.batteryState?.let {
            if (it == BatteryState.low) {
                deviceInfoData.station.add(
                    UIDeviceInfoItem(
                        resources.getString(R.string.battery_level),
                        resources.getString(R.string.battery_level_low),
                        warning = resources.getString(R.string.battery_level_low_message)
                    )
                )
            } else {
                deviceInfoData.station.add(
                    UIDeviceInfoItem(
                        resources.getString(R.string.battery_level),
                        resources.getString(R.string.battery_level_ok)
                    )
                )
            }
        }

        info.weatherStation?.hwVersion?.let {
            deviceInfoData.station.add(
                UIDeviceInfoItem(resources.getString(R.string.hardware_version), it)
            )
        }

        info.weatherStation?.lastActivity?.let {
            deviceInfoData.station.add(
                UIDeviceInfoItem(
                    resources.getString(R.string.last_weather_station_activity),
                    it.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))
                )
            )
        }

        info.gateway?.model?.let {
            deviceInfoData.gateway.add(
                UIDeviceInfoItem(resources.getString(R.string.model), it)
            )
        }

        info.gateway?.serialNumber?.let {
            deviceInfoData.gateway.add(
                UIDeviceInfoItem(resources.getString(R.string.device_serial_number), it.unmask())
            )
        }

        device.currentFirmware?.let { current ->
            val currentFirmware = if (device.currentFirmware.equals(device.assignedFirmware)) {
                current
            } else {
                "$current ${resources.getString(R.string.latest_hint)}"
            }
            deviceInfoData.gateway.add(
                UIDeviceInfoItem(
                    resources.getString(R.string.firmware_version), currentFirmware
                )
            )
        }

        info.gateway?.gpsSats?.let {
            deviceInfoData.gateway.add(
                UIDeviceInfoItem(resources.getString(R.string.gps_number_sats), it)
            )
        }

        info.gateway?.wifiRssi?.let {
            deviceInfoData.gateway.add(
                UIDeviceInfoItem(
                    resources.getString(R.string.wifi_rssi),
                    resources.getString(R.string.rssi, it)
                )
            )
        }

        info.gateway?.lastActivity?.let {
            deviceInfoData.gateway.add(
                UIDeviceInfoItem(
                    resources.getString(R.string.last_weather_station_activity),
                    it.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))
                )
            )
        }
    }

    fun parseDeviceInfoToShare(deviceInfo: UIDeviceInfo): String {
        var sharingText = String.empty()
        deviceInfo.default.forEach {
            sharingText += "${it}\n"
        }
        deviceInfo.gateway.forEach {
            sharingText += "${it}\n"
        }
        deviceInfo.station.forEach {
            sharingText += "${it}\n"
        }
        return sharingText
    }
}
