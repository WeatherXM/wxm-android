package com.weatherxm.ui.claimdevice.wifi

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.ApiError.DeviceNotFound
import com.weatherxm.data.models.ApiError.UserError.ClaimError.DeviceAlreadyClaimed
import com.weatherxm.data.models.ApiError.UserError.ClaimError.DeviceClaiming
import com.weatherxm.data.models.ApiError.UserError.ClaimError.InvalidClaimId
import com.weatherxm.data.models.ApiError.UserError.ClaimError.InvalidClaimLocation
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.Location
import com.weatherxm.ui.common.DeviceType
import com.weatherxm.ui.common.Resource
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.common.unmask
import com.weatherxm.usecases.ClaimDeviceUseCase
import com.weatherxm.util.Failure.getDefaultMessageResId
import com.weatherxm.util.Resources
import com.weatherxm.util.Validator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class ClaimWifiViewModel(
    val deviceType: DeviceType,
    private val claimDeviceUseCase: ClaimDeviceUseCase,
    private val resources: Resources,
    private val analytics: AnalyticsWrapper,
    private val dispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val onNext = MutableLiveData<Int>()
    private val onCancel = MutableLiveData(false)
    private val onClaimResult = MutableLiveData<Resource<UIDevice>>()
    private var currentSerialNumber: String = String.empty()
    private var currentClaimingKey: String? = null

    fun onNext() = onNext
    fun onCancel() = onCancel
    fun onClaimResult() = onClaimResult

    fun next(incrementPage: Int = 1) {
        onNext.postValue(incrementPage)
    }

    fun cancel() {
        onCancel.postValue(true)
    }

    fun setSerialNumber(serial: String) {
        currentSerialNumber = if (serial.contains(":")) serial.unmask() else serial
    }

    fun getSerialNumber(): String {
        return currentSerialNumber
    }

    fun setClaimingKey(key: String) {
        currentClaimingKey = key
    }

    fun getClaimingKey(): String? {
        return currentClaimingKey
    }

    fun validateSerial(serialNumber: String): Boolean {
        return Validator.validateSerialNumber(
            if (serialNumber.contains(":")) serialNumber.unmask() else serialNumber,
            deviceType
        )
    }

    fun validateClaimingKey(key: String): Boolean {
        return Validator.validateClaimingKey(key)
    }

    fun claimDevice(location: Location) {
        onClaimResult.postValue(Resource.loading())
        viewModelScope.launch(dispatcher) {
            claimDeviceUseCase.claimDevice(
                currentSerialNumber,
                location.lat,
                location.lon,
                currentClaimingKey
            ).onRight {
                Timber.d("Claimed device: $it")
                onClaimResult.postValue(Resource.success(it))
            }.onLeft {
                analytics.trackEventFailure(it.code)
                handleFailure(it)
            }
        }
    }

    private fun handleFailure(failure: Failure) {
        onClaimResult.postValue(
            Resource.error(
                resources.getString(
                    when (failure) {
                        is InvalidClaimId -> R.string.error_claim_invalid_serial
                        is InvalidClaimLocation -> R.string.error_claim_invalid_location
                        is DeviceAlreadyClaimed -> R.string.error_claim_already_claimed
                        is DeviceNotFound -> R.string.error_claim_not_found
                        is DeviceClaiming -> R.string.error_claim_device_claiming_error
                        else -> failure.getDefaultMessageResId()
                    }
                )
            )
        )
    }
}
