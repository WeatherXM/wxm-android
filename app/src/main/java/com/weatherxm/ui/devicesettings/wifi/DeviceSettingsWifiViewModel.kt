package com.weatherxm.ui.devicesettings.wifi

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import arrow.core.getOrElse
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.DeviceInfo
import com.weatherxm.data.models.RewardSplit
import com.weatherxm.data.models.WeatherStation
import com.weatherxm.ui.common.DeviceAlert
import com.weatherxm.ui.common.DeviceAlertType
import com.weatherxm.ui.common.RewardSplitsData
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.common.unmask
import com.weatherxm.ui.devicesettings.BaseDeviceSettingsViewModel
import com.weatherxm.ui.devicesettings.UIDeviceInfo
import com.weatherxm.ui.devicesettings.UIDeviceInfoItem
import com.weatherxm.usecases.AuthUseCase
import com.weatherxm.usecases.StationSettingsUseCase
import com.weatherxm.usecases.UserUseCase
import com.weatherxm.util.DateTimeHelper.getFormattedDateAndTime
import com.weatherxm.util.Resources
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

class DeviceSettingsWifiViewModel(
    device: UIDevice,
    private val usecase: StationSettingsUseCase,
    private val resources: Resources,
    private val userUseCase: UserUseCase,
    private val authUseCase: AuthUseCase,
    private val analytics: AnalyticsWrapper
) : BaseDeviceSettingsViewModel(device, usecase, resources, analytics) {
    private val onDeviceInfo = MutableLiveData<UIDeviceInfo>()

    private val data =
        UIDeviceInfo(mutableListOf(), mutableListOf(), mutableListOf(), null)

    fun onDeviceInfo(): LiveData<UIDeviceInfo> = onDeviceInfo

    override fun getDeviceInformation(context: Context) {
        data.default.add(
            UIDeviceInfoItem(resources.getString(R.string.station_default_name), device.name)
        )

        device.bundleTitle?.let {
            data.default.add(UIDeviceInfoItem(resources.getString(R.string.bundle_name), it))
        }
        device.claimedAt?.let {
            data.default.add(
                UIDeviceInfoItem(
                    resources.getString(R.string.claimed_at),
                    it.getFormattedDateAndTime(context)
                )
            )
        }
        onLoading.postValue(true)
        viewModelScope.launch {
            usecase.getDeviceInfo(device.id).onLeft {
                analytics.trackEventFailure(it.code)
                Timber.d("$it: Fetching remote device info failed for device: $device")
                onDeviceInfo.postValue(data)
            }.onRight { info ->
                Timber.d("Got device info: $info")
                handleInfo(context, info)
                onDeviceInfo.postValue(data)
            }
            onLoading.postValue(false)
        }
    }

    override suspend fun handleInfo(context: Context, info: DeviceInfo) {
        handleRewardSplitInfo(info.rewardSplit ?: emptyList())

        // Get gateway info
        info.gateway?.apply {
            model?.let {
                data.gateway.add(UIDeviceInfoItem(resources.getString(R.string.model), it))
            }

            serialNumber?.let {
                data.gateway.add(
                    UIDeviceInfoItem(resources.getString(R.string.serial_number), it.unmask())
                )
            }

            handleFirmwareInfo()

            val gpsTimestamp =
                gpsSatsLastActivity?.getFormattedDateAndTime(context) ?: String.empty()
            gpsSats?.let {
                data.gateway.add(
                    UIDeviceInfoItem(
                        resources.getString(R.string.gps_number_sats),
                        resources.getString(R.string.satellites, it, gpsTimestamp)
                    )
                )
            }

            val wifiTimestamp =
                wifiRssiLastActivity?.getFormattedDateAndTime(context) ?: String.empty()
            wifiRssi?.let {
                data.gateway.add(
                    UIDeviceInfoItem(
                        resources.getString(R.string.wifi_rssi),
                        resources.getString(R.string.rssi, it, wifiTimestamp)
                    )
                )
            }

            lastActivity?.let {
                data.gateway.add(
                    UIDeviceInfoItem(
                        resources.getString(R.string.last_gateway_activity),
                        it.getFormattedDateAndTime(context)
                    )
                )
            }
        }

        // Get weather station info
        handleWeatherStationInfo(context, info.weatherStation)
    }

    @Suppress("MagicNumber")
    private fun handleWeatherStationInfo(context: Context, weatherStation: WeatherStation?) {
        weatherStation?.apply {
            model?.let {
                data.station.add(UIDeviceInfoItem(resources.getString(R.string.model), it))
            }

            hwVersion?.let {
                data.station.add(
                    UIDeviceInfoItem(resources.getString(R.string.hardware_version), it)
                )
            }

            val lastStationRssiTs =
                stationRssiLastActivity?.getFormattedDateAndTime(context) ?: String.empty()
            stationRssi?.let {
                val deviceAlert = if (it >= -80) {
                    null
                } else if (it >= -95) {
                    DeviceAlert.createWarning(DeviceAlertType.LOW_STATION_RSSI)
                } else {
                    DeviceAlert.createError(DeviceAlertType.LOW_STATION_RSSI)
                }
                data.station.add(
                    UIDeviceInfoItem(
                        resources.getString(R.string.station_gateway_rssi),
                        resources.getString(R.string.rssi, it.toString(), lastStationRssiTs),
                        deviceAlert
                    )
                )
            }

            batteryState?.let {
                handleLowBatteryInfo(data.station, it)
            }

            lastActivity?.let {
                data.station.add(
                    UIDeviceInfoItem(
                        resources.getString(R.string.last_weather_station_activity),
                        it.getFormattedDateAndTime(context)
                    )
                )
            }
        }
    }

    private suspend fun handleRewardSplitInfo(splits: List<RewardSplit>) {
        var walletAddress = String.empty()
        coroutineScope {
            val getWalletAddressJob = launch {
                val isLoggedIn = authUseCase.isLoggedIn().getOrElse { false }
                if (isLoggedIn) {
                    userUseCase.getWalletAddress().onRight {
                        walletAddress = it
                    }
                }
            }
            getWalletAddressJob.join()
            data.rewardSplit = RewardSplitsData(splits, walletAddress)
        }
    }

    private fun handleFirmwareInfo() {
        device.currentFirmware?.let { current ->
            val currentFirmware = if (device.currentFirmware.equals(device.assignedFirmware)) {
                "$current ${resources.getString(R.string.latest_hint)}"
            } else {
                current
            }
            data.gateway.add(
                UIDeviceInfoItem(resources.getString(R.string.firmware_version), currentFirmware)
            )
        }
    }
}
