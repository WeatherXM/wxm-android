package com.weatherxm.ui.stationsettings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import com.weatherxm.R
import com.weatherxm.data.ApiError
import com.weatherxm.data.BatteryState
import com.weatherxm.data.Device
import com.weatherxm.data.DeviceProfile
import com.weatherxm.ui.common.UIError
import com.weatherxm.ui.common.capitalizeWords
import com.weatherxm.ui.common.unmask
import com.weatherxm.usecases.StationSettingsUseCase
import com.weatherxm.util.ResourcesHelper
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class StationSettingsViewModel(var device: Device) : ViewModel(), KoinComponent {
    private val usecase: StationSettingsUseCase by inject()
    private val resHelper: ResourcesHelper by inject()

    private val onEditNameChange = MutableLiveData<String>()
    private val onDeviceRemoved = MutableLiveData<Boolean>()
    private val onStationInfo = MutableLiveData<List<StationInfo>>()
    private val onError = MutableLiveData<UIError>()
    private val onLoading = MutableLiveData<Boolean>()

    fun onEditNameChange(): LiveData<String> = onEditNameChange
    fun onDeviceRemoved(): LiveData<Boolean> = onDeviceRemoved
    fun onStationInfo(): LiveData<List<StationInfo>> = onStationInfo
    fun onError(): LiveData<UIError> = onError
    fun onLoading(): LiveData<Boolean> = onLoading

    fun canChangeFriendlyName(): Either<UIError, Boolean> {
        return usecase.canChangeFriendlyName(device.id)
            .mapLeft {
                Timber.d(it.message)
                UIError(resHelper.getString(R.string.error_friendly_name_change_rate_limit))
            }
    }

    fun setOrClearFriendlyName(friendlyName: String?) {
        if (friendlyName == null) {
            clearFriendlyName()
        } else {
            setFriendlyName(friendlyName)
        }
    }

    private fun setFriendlyName(friendlyName: String) {
        if (friendlyName.isNotEmpty() && friendlyName != device.attributes?.friendlyName) {
            onLoading.postValue(true)
            viewModelScope.launch {
                usecase.setFriendlyName(device.id, friendlyName)
                    .map {
                        onEditNameChange.postValue(friendlyName)
                    }
                    .mapLeft {
                        onError.postValue(
                            UIError(resHelper.getString(R.string.error_reach_out_short))
                        )
                    }
                onLoading.postValue(false)
            }
        }
    }

    private fun clearFriendlyName() {
        if (device.attributes?.friendlyName?.isNotEmpty() == true) {
            onLoading.postValue(true)
            viewModelScope.launch {
                usecase.clearFriendlyName(device.id)
                    .map {
                        onEditNameChange.postValue(device.name)
                    }
                    .mapLeft {
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
                usecase.removeDevice(serialNumber.unmask())
                    .map {
                        Timber.d("Device ${device.name} removed.")
                        onDeviceRemoved.postValue(true)
                    }
                    .mapLeft {
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

    fun getStationInformation() {
        val stationInfo = getStationInfoFromDevice()
        onLoading.postValue(true)
        viewModelScope.launch {
            usecase.getDeviceInfo(device.id).onLeft {
                Timber.d("$it: Fetching remote device info failed for device: $device")
                onStationInfo.postValue(stationInfo)
            }.onRight { deviceInfo ->
                Timber.d("Got device info: $deviceInfo")
                deviceInfo.weatherStation?.batteryState?.let {
                    if (it == BatteryState.low) {
                        stationInfo.add(
                            1,
                            StationInfo(
                                resHelper.getString(R.string.battery_level),
                                resHelper.getString(R.string.battery_level_low),
                                warning = resHelper.getString(R.string.battery_level_low_message)
                            )
                        )
                    } else {
                        stationInfo.add(
                            1,
                            StationInfo(
                                resHelper.getString(R.string.battery_level),
                                resHelper.getString(R.string.battery_level_ok)
                            )
                        )
                    }
                }

                deviceInfo.weatherStation?.hwVersion?.let {
                    stationInfo.add(StationInfo(resHelper.getString(R.string.hardware_version), it))
                }
                deviceInfo.weatherStation?.lastHotspot?.let {
                    stationInfo.add(
                        StationInfo(
                            resHelper.getString(R.string.last_hotspot),
                            it.replace("-", " ").capitalizeWords()
                        )
                    )
                }
                deviceInfo.weatherStation?.lastTxRssi?.let {
                    stationInfo.add(
                        StationInfo(
                            resHelper.getString(R.string.last_tx_rssi),
                            resHelper.getString(R.string.rssi, it)
                        )
                    )
                }
                deviceInfo.gateway?.gpsSats?.let {
                    stationInfo.add(StationInfo(resHelper.getString(R.string.gps_number_sats), it))
                }
                deviceInfo.gateway?.wifiRssi?.let {
                    stationInfo.add(
                        StationInfo(
                            resHelper.getString(R.string.wifi_rssi),
                            resHelper.getString(R.string.rssi, it)
                        )
                    )
                }
                onStationInfo.postValue(stationInfo)
            }
            onLoading.postValue(false)
        }
    }

    private fun getStationInfoFromDevice(): MutableList<StationInfo> {
        return mutableListOf<StationInfo>().apply {
            add(StationInfo(resHelper.getString(R.string.station_default_name), device.name))
            if (device.profile == DeviceProfile.Helium) {
                device.label?.unmask()?.let {
                    add(StationInfo(resHelper.getString(R.string.dev_eui), it))
                }
            } else {
                device.label?.unmask()?.let {
                    add(StationInfo(resHelper.getString(R.string.device_serial_number_title), it))
                }
            }
            device.attributes?.firmware?.current?.let { current ->
                val assigned = device.attributes?.firmware?.assigned
                if (!assigned.isNullOrEmpty() && current != assigned
                    && usecase.shouldShowOTAPrompt(device)
                ) {
                    add(
                        StationInfo(
                            resHelper.getString(R.string.firmware_version),
                            "$current ➞ $assigned",
                            StationAction(
                                resHelper.getString(R.string.action_update_firmware),
                                ActionType.UPDATE_FIRMWARE
                            )
                        )
                    )
                } else {
                    add(StationInfo(resHelper.getString(R.string.firmware_version), current))
                }
            }
        }
    }

    fun parseStationInfoToShare(stationInfo: List<StationInfo>): String {
        var sharingText = ""
        stationInfo.forEach {
            sharingText += "${it}\n"
        }
        return sharingText
    }
}
