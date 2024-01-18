package com.weatherxm.ui.claimdevice.m5

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.data.ApiError.DeviceNotFound
import com.weatherxm.data.ApiError.UserError.ClaimError.DeviceAlreadyClaimed
import com.weatherxm.data.ApiError.UserError.ClaimError.DeviceClaiming
import com.weatherxm.data.ApiError.UserError.ClaimError.InvalidClaimId
import com.weatherxm.data.ApiError.UserError.ClaimError.InvalidClaimLocation
import com.weatherxm.data.Failure
import com.weatherxm.data.Location
import com.weatherxm.data.Resource
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.usecases.ClaimDeviceUseCase
import com.weatherxm.util.Analytics
import com.weatherxm.util.Failure.getDefaultMessageResId
import com.weatherxm.util.Resources
import kotlinx.coroutines.launch
import timber.log.Timber

class ClaimM5ViewModel(
    private val claimDeviceUseCase: ClaimDeviceUseCase,
    private val resources: Resources,
    private val analytics: Analytics
) : ViewModel() {

    private val onNext = MutableLiveData(false)
    private val onCancel = MutableLiveData(false)
    private val onClaimResult = MutableLiveData<Resource<UIDevice>>().apply {
        value = Resource.loading()
    }

    fun onNext() = onNext
    fun onCancel() = onCancel
    fun onClaimResult() = onClaimResult

    fun next() {
        onNext.postValue(true)
    }

    fun cancel() {
        onCancel.postValue(true)
    }

    fun claimDevice(serialNumber: String, location: Location) {
        onClaimResult.postValue(Resource.loading())
        viewModelScope.launch {
            claimDeviceUseCase.claimDevice(serialNumber, location.lat, location.lon)
                .map {
                    Timber.d("Claimed device: $it")
                    onClaimResult.postValue(Resource.success(it))
                }
                .mapLeft {
                    analytics.trackEventFailure(it.code)
                    handleFailure(it)
                }
        }
    }

    private fun handleFailure(failure: Failure) {
        onClaimResult.postValue(
            Resource.error(
                msg = resources.getString(
                    when (failure) {
                        is InvalidClaimId -> R.string.error_claim_invalid_serial
                        is InvalidClaimLocation -> R.string.error_claim_invalid_location
                        is DeviceAlreadyClaimed -> R.string.error_claim_already_claimed
                        is DeviceNotFound -> R.string.error_claim_not_found
                        is DeviceClaiming -> R.string.error_claim_not_found_helium
                        else -> failure.getDefaultMessageResId()
                    }
                ),
                error = failure
            )
        )
    }
}
