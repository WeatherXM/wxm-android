package com.weatherxm.ui.devicesettings.helium

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.BatteryState
import com.weatherxm.data.DeviceInfo
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.capitalizeWords
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.devicesettings.ActionType
import com.weatherxm.ui.devicesettings.BaseDeviceSettingsViewModel
import com.weatherxm.ui.devicesettings.UIDeviceAction
import com.weatherxm.ui.devicesettings.UIDeviceInfo
import com.weatherxm.ui.devicesettings.UIDeviceInfoItem
import com.weatherxm.usecases.StationSettingsUseCase
import com.weatherxm.util.Resources
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class DeviceSettingsHeliumViewModel(
    device: UIDevice,
    private val usecase: StationSettingsUseCase,
    private val resources: Resources,
    private val analytics: AnalyticsWrapper
) : BaseDeviceSettingsViewModel(device, usecase, resources, analytics) {
    private val onDeviceInfo = MutableLiveData<UIDeviceInfo>()

    private val deviceInfoData = UIDeviceInfo(mutableListOf(), mutableListOf(), mutableListOf())

    fun onDeviceInfo(): LiveData<UIDeviceInfo> = onDeviceInfo

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
                handleInfo(info)
                onDeviceInfo.postValue(deviceInfoData)
            }
            onLoading.postValue(false)
        }
    }

    /**
     * Suppressing LongMethod because it's just a bunch of `let` statements
     * and adding items in the `deviceInfoData` list
     */
    @Suppress("LongMethod")
    override fun handleInfo(info: DeviceInfo) {
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

    override fun parseDeviceInfoToShare(deviceInfo: UIDeviceInfo): String {
        var sharingText = String.empty()
        deviceInfo.default.forEach {
            sharingText += "${it}\n"
        }
        return sharingText
    }
}
