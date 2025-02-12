package com.weatherxm.ui.devicesettings.helium

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.DeviceInfo
import com.weatherxm.data.models.RewardSplit
import com.weatherxm.ui.common.DeviceAlert
import com.weatherxm.ui.common.DeviceAlertType
import com.weatherxm.ui.common.RewardSplitsData
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.capitalizeWords
import com.weatherxm.ui.common.empty
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

@Suppress("LongParameterList")
class DeviceSettingsHeliumViewModel(
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
        device.wsModel?.let {
            data.default.add(UIDeviceInfoItem(resources.getString(R.string.model), it))
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

    override suspend fun handleInfo(context: Context, info: DeviceInfo) {
        handleRewardSplitInfo(info.rewardSplit ?: emptyList())

        info.weatherStation?.apply {
            devEUI?.let {
                data.default.add(UIDeviceInfoItem(resources.getString(R.string.dev_eui), it))
            }

            handleFirmwareInfo()

            hwVersion?.let {
                data.default.add(
                    UIDeviceInfoItem(resources.getString(R.string.hardware_version), it)
                )
            }

            batteryState?.let {
                handleLowBatteryInfo(data.default, it)
            }

            val lastHotspotTimestamp =
                lastHotspotLastActivity?.getFormattedDateAndTime(context) ?: String.empty()
            lastHotspot?.let {
                data.default.add(
                    UIDeviceInfoItem(
                        resources.getString(R.string.last_hotspot),
                        "${it.replace("-", " ").capitalizeWords()} @ $lastHotspotTimestamp"
                    )
                )
            }

            val lastTxTimestamp =
                lastTxRssiLastActivity?.getFormattedDateAndTime(context) ?: String.empty()
            lastTxRssi?.let {
                data.default.add(
                    UIDeviceInfoItem(
                        resources.getString(R.string.last_tx_rssi),
                        resources.getString(R.string.rssi, it, lastTxTimestamp)
                    )
                )
            }

            lastActivity?.let {
                data.default.add(
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

    private fun handleFirmwareInfo() {
        device.currentFirmware?.let { current ->
            if (usecase.shouldNotifyOTA(device) && device.shouldPromptUpdate()) {
                data.default.add(
                    UIDeviceInfoItem(
                        title = resources.getString(R.string.firmware_version),
                        value = "$current ➞ ${device.assignedFirmware}",
                        deviceAlert = DeviceAlert.createWarning(DeviceAlertType.NEEDS_UPDATE)
                    )
                )
            } else {
                val currentFirmware = if (device.currentFirmware.equals(device.assignedFirmware)) {
                    "$current ${resources.getString(R.string.latest_hint)}"
                } else {
                    current
                }
                data.default.add(
                    UIDeviceInfoItem(
                        resources.getString(R.string.firmware_version), currentFirmware
                    )
                )
            }
        }
    }
}
