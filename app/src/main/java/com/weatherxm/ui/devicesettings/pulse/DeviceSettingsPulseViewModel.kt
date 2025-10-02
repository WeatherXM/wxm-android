package com.weatherxm.ui.devicesettings.pulse

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.BatteryState
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
import com.weatherxm.usecases.DevicePhotoUseCase
import com.weatherxm.usecases.StationSettingsUseCase
import com.weatherxm.usecases.UserUseCase
import com.weatherxm.util.DateTimeHelper.getFormattedDateAndTime
import com.weatherxm.util.Resources
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

@Suppress("LongParameterList")
class DeviceSettingsPulseViewModel(
    device: UIDevice,
    private val usecase: StationSettingsUseCase,
    photosUseCase: DevicePhotoUseCase,
    private val userUseCase: UserUseCase,
    private val authUseCase: AuthUseCase,
    private val resources: Resources,
    private val analytics: AnalyticsWrapper,
    dispatcher: CoroutineDispatcher
) : BaseDeviceSettingsViewModel(device, usecase, photosUseCase, resources, analytics, dispatcher) {
    private val onDeviceInfo = MutableLiveData<UIDeviceInfo>()

    private lateinit var data: UIDeviceInfo

    fun onDeviceInfo(): LiveData<UIDeviceInfo> = onDeviceInfo

    override fun getDeviceInformation(context: Context) {
        data = UIDeviceInfo.empty()

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
        viewModelScope.launch(dispatcher) {
            usecase.getDeviceInfo(device.id).onLeft {
                analytics.trackEventFailure(it.code)
                Timber.d("$it: Fetching remote device info failed for device: $device")
                onDeviceInfo.postValue(data)
            }.onRight { info ->
                Timber.d("Got device info: $info")
                handleInfo(context, info)
                onDeviceInfo.postValue(data)
            }
            super.getDevicePhotos()
            onLoading.postValue(false)
        }
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod")
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

            val networkTimestamp =
                networkRssiLastActivity?.getFormattedDateAndTime(context) ?: String.empty()
            networkRssi?.let {
                data.gateway.add(
                    UIDeviceInfoItem(
                        resources.getString(R.string.gsm_signal),
                        resources.getString(R.string.rssi, it, networkTimestamp)
                    )
                )
            }

            frequency?.let {
                data.gateway.add(
                    UIDeviceInfoItem(
                        resources.getString(R.string.gateway_frequency),
                        "$it ${resources.getString(R.string.mhz)}"
                    )
                )
            }

            sim?.iccid?.let {
                data.gateway.add(
                    UIDeviceInfoItem(
                        resources.getString(R.string.external_sim),
                        resources.getString(R.string.external_sim_is_in_use)
                    )
                )
                data.gateway.add(
                    UIDeviceInfoItem(resources.getString(R.string.iccid_external_sim), it)
                )
            }

            if (sim?.mcc != null && sim.mnc != null) {
                val mccData = "${resources.getString(R.string.mcc)}: ${sim.mcc}"
                val mncData = "${resources.getString(R.string.mnc)}: ${sim.mnc}"
                data.gateway.add(
                    UIDeviceInfoItem(
                        resources.getString(R.string.mobile_country_network_code),
                        "$mccData - $mncData"
                    )
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

            batteryState?.let {
                if (it == BatteryState.low) {
                    data.gateway.add(
                        UIDeviceInfoItem(
                            resources.getString(R.string.gateway_battery_level),
                            resources.getString(R.string.battery_level_low),
                            DeviceAlert.createWarning(DeviceAlertType.LOW_GATEWAY_BATTERY)
                        )
                    )
                } else {
                    data.gateway.add(
                        UIDeviceInfoItem(
                            resources.getString(R.string.gateway_battery_level),
                            resources.getString(R.string.battery_level_ok)
                        )
                    )
                }
            }

            lastActivity?.let {
                data.gateway.add(
                    UIDeviceInfoItem(
                        resources.getString(R.string.last_gateway_activity),
                        it.getFormattedDateAndTime(context)
                    )
                )
            }

            val gatewayRssiTimestamp =
                gatewayRssiLastActivity?.getFormattedDateAndTime(context) ?: String.empty()
            gatewayRssi?.let {
                data.gateway.add(
                    UIDeviceInfoItem(
                        resources.getString(R.string.signal_gateway_station),
                        resources.getString(R.string.rssi, it, gatewayRssiTimestamp)
                    )
                )
            }

            nextCommunication?.let {
                data.gateway.add(
                    UIDeviceInfoItem(
                        resources.getString(R.string.next_communication),
                        it.getFormattedDateAndTime(context)
                    )
                )
            }
        }

        // Get weather station info
        handleWeatherStationInfo(context, info.weatherStation)
    }

    private suspend fun handleRewardSplitInfo(splits: List<RewardSplit>) {
        var walletAddress = String.empty()
        coroutineScope {
            val getWalletAddressJob = launch {
                if (authUseCase.isLoggedIn()) {
                    userUseCase.getWalletAddress().onRight {
                        walletAddress = it
                    }
                }
            }
            getWalletAddressJob.join()
            data.rewardSplit = RewardSplitsData(splits, walletAddress)
        }
    }

    @Suppress("MagicNumber")
    private fun handleWeatherStationInfo(context: Context, weatherStation: WeatherStation?) {
        weatherStation?.apply {
            model?.let {
                data.station.add(UIDeviceInfoItem(resources.getString(R.string.model), it))
            }

            id?.let {
                data.station.add(UIDeviceInfoItem(resources.getString(R.string.id), it))
            }

            batteryState?.let {
                if (it == BatteryState.low) {
                    data.station.add(
                        UIDeviceInfoItem(
                            resources.getString(R.string.station_battery_level),
                            resources.getString(R.string.battery_level_low),
                            deviceAlert = DeviceAlert.createWarning(DeviceAlertType.LOW_BATTERY)
                        )
                    )
                } else {
                    data.station.add(
                        UIDeviceInfoItem(
                            resources.getString(R.string.station_battery_level),
                            resources.getString(R.string.battery_level_ok)
                        )
                    )
                }
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
