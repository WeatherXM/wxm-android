package com.weatherxm.ui.claimdevice.helium

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.data.ApiError.DeviceNotFound
import com.weatherxm.data.ApiError.UserError.ClaimError.DeviceAlreadyClaimed
import com.weatherxm.data.ApiError.UserError.ClaimError.DeviceClaiming
import com.weatherxm.data.ApiError.UserError.ClaimError.InvalidClaimId
import com.weatherxm.data.ApiError.UserError.ClaimError.InvalidClaimLocation
import com.weatherxm.data.Frequency
import com.weatherxm.data.Location
import com.weatherxm.data.Resource
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.usecases.BluetoothConnectionUseCase
import com.weatherxm.usecases.ClaimDeviceUseCase
import com.weatherxm.util.Analytics
import com.weatherxm.util.Failure.getDefaultMessageResId
import com.weatherxm.util.Resources
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber

// Suppress this as almost all functions are get/set methods
@Suppress("TooManyFunctions")
class ClaimHeliumViewModel(
    private val claimDeviceUseCase: ClaimDeviceUseCase,
    private val resources: Resources,
    private val analytics: Analytics
) : ViewModel() {
    private val onCancel = MutableLiveData(false)
    private val onNext = MutableLiveData(false)
    private val onBackToLocation = MutableLiveData(false)
    private val onClaimResult = MutableLiveData<Resource<UIDevice>>().apply {
        value = Resource.loading()
    }

    fun onCancel() = onCancel
    fun onNext() = onNext
    fun onBackToLocation() = onBackToLocation
    fun onClaimResult() = onClaimResult

    private var devEUI: String = ""
    private var deviceKey: String = ""
    private var frequency: Frequency = Frequency.US915

    fun setFrequency(frequency: Frequency) {
        this.frequency = frequency
    }

    fun setDeviceEUI(devEUI: String) {
        this.devEUI = devEUI
    }

    fun setDeviceKey(key: String) {
        deviceKey = key
    }

    fun getFrequency(): Frequency {
        return frequency
    }

    fun getDevEUI(): String {
        return devEUI
    }

    fun getDeviceKey(): String {
        return deviceKey
    }

    fun cancel() {
        onCancel.postValue(true)
    }

    fun next() {
        onNext.postValue(true)
    }

    fun backToLocation() {
        onBackToLocation.postValue(true)
    }

    fun claimDevice(location: Location) {
        onClaimResult.postValue(Resource.loading())
        viewModelScope.launch {
            claimDeviceUseCase.claimDevice(devEUI, location.lat, location.lon, deviceKey)
                .map {
                    Timber.d("Claimed device: $it")
                    onClaimResult.postValue(Resource.success(it))
                }
                .mapLeft {
                    analytics.trackEventFailure(it.code)
                    onClaimResult.postValue(
                        Resource.error(
                            msg = resources.getString(
                                when (it) {
                                    is InvalidClaimId -> R.string.error_claim_invalid_dev_eui
                                    is InvalidClaimLocation -> R.string.error_claim_invalid_location
                                    is DeviceAlreadyClaimed -> R.string.error_claim_already_claimed
                                    is DeviceNotFound -> R.string.error_claim_not_found_helium
                                    is DeviceClaiming -> R.string.error_claim_device_claiming_error
                                    else -> it.getDefaultMessageResId()
                                }
                            ),
                            error = it
                        )
                    )
                }
        }
    }

    fun setClaimedDevice(device: UIDevice?) {
        device?.let { onClaimResult.postValue(Resource.success(device)) }
    }
}
