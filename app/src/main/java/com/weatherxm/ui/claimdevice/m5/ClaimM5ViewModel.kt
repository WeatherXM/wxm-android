package com.weatherxm.ui.claimdevice.m5

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
import com.weatherxm.data.Failure
import com.weatherxm.data.Resource
import com.weatherxm.usecases.ClaimDeviceUseCase
import com.weatherxm.util.ResourcesHelper
import com.weatherxm.util.UIErrors.getDefaultMessageResId
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class ClaimM5ViewModel : ViewModel(), KoinComponent {
    private val claimDeviceUseCase: ClaimDeviceUseCase by inject()
    private val resHelper: ResourcesHelper by inject()

    private var userEmail: String? = null

    private val onNext = MutableLiveData(false)
    private val onCancel = MutableLiveData(false)
    private val onClaimResult = MutableLiveData<Resource<Device>>().apply {
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

    fun fetchUserEmail() {
        viewModelScope.launch {
            claimDeviceUseCase.fetchUserEmail()
                .map {
                    userEmail = it
                }
                .mapLeft {
                    userEmail = null
                }
        }
    }

    fun getUserEmail(): String? {
        return userEmail
    }

    fun claimDevice(serialNumber: String, location: Location) {
        onClaimResult.postValue(Resource.loading())
        viewModelScope.launch {
            claimDeviceUseCase.claimDevice(serialNumber, location.latitude, location.longitude)
                .map {
                    Timber.d("Claimed device: $it")
                    onClaimResult.postValue(Resource.success(it))
                }
                .mapLeft {
                    handleFailure(it)
                }
        }
    }

    private fun handleFailure(failure: Failure) {
        onClaimResult.postValue(
            Resource.error(
                resHelper.getString(
                    when (failure) {
                        is InvalidClaimId -> R.string.error_claim_invalid_serial
                        is InvalidClaimLocation -> R.string.error_claim_invalid_location
                        is DeviceAlreadyClaimed -> R.string.error_claim_already_claimed
                        is DeviceNotFound -> R.string.error_claim_not_found
                        is DeviceClaiming -> R.string.error_claim_not_found_helium
                        else -> failure.getDefaultMessageResId()
                    }
                ),
                failure
            )
        )
    }
}
