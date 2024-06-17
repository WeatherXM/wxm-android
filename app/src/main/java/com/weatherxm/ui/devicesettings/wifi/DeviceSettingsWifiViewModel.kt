package com.weatherxm.ui.devicesettings.wifi

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.BatteryState
import com.weatherxm.data.DeviceInfo
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.common.unmask
import com.weatherxm.ui.devicesettings.BaseDeviceSettingsViewModel
import com.weatherxm.ui.devicesettings.UIDeviceInfo
import com.weatherxm.ui.devicesettings.UIDeviceInfoItem
import com.weatherxm.usecases.StationSettingsUseCase
import com.weatherxm.util.Resources
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class DeviceSettingsWifiViewModel(
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
                UIDeviceInfoItem(resources.getString(R.string.serial_number), it.unmask())
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
                    resources.getString(R.string.last_gateway_activity),
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
        deviceInfo.gateway.forEach {
            sharingText += "${it}\n"
        }
        deviceInfo.station.forEach {
            sharingText += "${it}\n"
        }
        return sharingText
    }
}
