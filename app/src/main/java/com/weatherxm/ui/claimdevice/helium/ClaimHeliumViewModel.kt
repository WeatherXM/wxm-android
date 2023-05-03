package com.weatherxm.ui.claimdevice.helium

import android.location.Location
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.data.ApiError.DeviceNotFound
import com.weatherxm.data.ApiError.UserError.ClaimError.DeviceAlreadyClaimed
import com.weatherxm.data.ApiError.UserError.ClaimError.DeviceClaiming
import com.weatherxm.data.ApiError.UserError.ClaimError.InvalidClaimId
import com.weatherxm.data.ApiError.UserError.ClaimError.InvalidClaimLocation
import com.weatherxm.data.Device
import com.weatherxm.data.Frequency
import com.weatherxm.data.Resource
import com.weatherxm.usecases.BluetoothConnectionUseCase
import com.weatherxm.usecases.ClaimDeviceUseCase
import com.weatherxm.util.ResourcesHelper
import com.weatherxm.util.UIErrors.getDefaultMessageResId
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

// Suppress this as almost all functions are get/set methods
@Suppress("TooManyFunctions")
class ClaimHeliumViewModel : ViewModel(), KoinComponent {
    private val claimDeviceUseCase: ClaimDeviceUseCase by inject()
    private val connectionUseCase: BluetoothConnectionUseCase by inject()
    private val resHelper: ResourcesHelper by inject()

    private val onCancel = MutableLiveData(false)
    private val onNext = MutableLiveData(false)
    private val onBackToLocation = MutableLiveData(false)
    private val onClaimResult = MutableLiveData<Resource<Device>>().apply {
        value = Resource.loading()
    }

    fun onCancel() = onCancel
    fun onNext() = onNext
    fun onBackToLocation() = onBackToLocation
    fun onClaimResult() = onClaimResult

    private var userEmail: String? = null
    private var devEUI: String = ""
    private var deviceKey: String = ""
    private var frequency: Frequency = Frequency.US915

    @OptIn(DelicateCoroutinesApi::class)
    fun disconnectFromPeripheral() {
        GlobalScope.launch {
            connectionUseCase.disconnectFromPeripheral()
        }
    }

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

    fun fetchUserEmail() {
        viewModelScope.launch {
            claimDeviceUseCase.fetchUserEmail().map {
                userEmail = it
            }.mapLeft {
                userEmail = null
            }
        }
    }

    fun getUserEmail(): String? {
        return userEmail
    }

    fun claimDevice(location: Location) {
        onClaimResult.postValue(Resource.loading())
        viewModelScope.launch {
            claimDeviceUseCase.claimDevice(devEUI, location.latitude, location.longitude, deviceKey)
                .map {
                    Timber.d("Claimed device: $it")
                    onClaimResult.postValue(Resource.success(it))
                }
                .mapLeft {
                    onClaimResult.postValue(
                        Resource.error(
                            msg = resHelper.getString(
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

    fun setClaimedDevice(device: Device?) {
        device?.let { onClaimResult.postValue(Resource.success(device)) }
    }
}
