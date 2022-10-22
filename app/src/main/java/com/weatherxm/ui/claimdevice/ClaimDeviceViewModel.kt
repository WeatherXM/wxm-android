package com.weatherxm.ui.claimdevice

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.data.ApiError.DeviceNotFound
import com.weatherxm.data.ApiError.UserError.ClaimError.DeviceAlreadyClaimed
import com.weatherxm.data.ApiError.UserError.ClaimError.InvalidClaimId
import com.weatherxm.data.ApiError.UserError.ClaimError.InvalidClaimLocation
import com.weatherxm.data.Failure
import com.weatherxm.data.Resource
import com.weatherxm.usecases.ClaimDeviceUseCase
import com.weatherxm.util.ResourcesHelper
import com.weatherxm.util.UIErrors.getDefaultMessageResId
import com.weatherxm.util.Validator
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

/*
* This suppress is needed because of the complexity of the claiming process where a lot of
* fragments and an activity are involved and communication is needed between them
*/
@Suppress("TooManyFunctions")
class ClaimDeviceViewModel : ViewModel(), KoinComponent {
    private val claimDeviceUseCase: ClaimDeviceUseCase by inject()
    private val resHelper: ResourcesHelper by inject()
    private val validator: Validator by inject()

    private var currentSerialNumber: String = ""
    private var isSerialSet = false
    private var userEmail: String? = null
    private var installationLat: Double = 0.0
    private var installationLon: Double = 0.0

    private val onNextButtonEnabledStatus = MutableLiveData(true)
    private val onNextButtonClick = MutableLiveData(false)
    private val onCancel = MutableLiveData(false)
    private val onCheckSerialAndContinue = MutableLiveData(false)
    private val onClaimResult = MutableLiveData<Resource<String>>().apply {
        value = Resource.loading()
    }

    fun onNextButtonEnabledStatus() = onNextButtonEnabledStatus
    fun onNextButtonClick() = onNextButtonClick
    fun onCancel() = onCancel
    fun onCheckSerialAndContinue() = onCheckSerialAndContinue
    fun onClaimResult() = onClaimResult

    fun cancel() {
        onCancel.postValue(true)
    }

    fun setSerialSet(isSet: Boolean) {
        isSerialSet = isSet
    }

    fun getSerialNumber(): String {
        return currentSerialNumber
    }

    fun getUserEmail(): String? {
        return userEmail
    }

    fun setInstallationLocation(lat: Double, lon: Double) {
        installationLat = lat
        installationLat = lon
    }

    fun claimDevice() {
        onClaimResult.postValue(Resource.loading())
        viewModelScope.launch {
            claimDeviceUseCase.claimDevice(currentSerialNumber, installationLat, installationLon)
                .map {
                    Timber.d("Claimed device: $it")
                    onClaimResult.postValue(Resource.success(it.name))
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
                        is DeviceAlreadyClaimed -> R.string.error_claim_device_already_claimed
                        is DeviceNotFound -> R.string.error_claim_not_found
                        else -> failure.getDefaultMessageResId()
                    }
                )
            )
        )
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

    fun nextButtonStatus(enabled: Boolean) {
        onNextButtonEnabledStatus.postValue(enabled)
    }

    fun nextButtonClick() {
        onNextButtonClick.postValue(true)
    }

    fun isSerialSet(): Boolean {
        return isSerialSet
    }

    fun checkSerialAndContinue() {
        onCheckSerialAndContinue.postValue(true)
    }

    fun validateAndSetSerial(serialNumber: String): Boolean {
        return validator.validateSerialNumber(serialNumber).apply {
            if (this) {
                currentSerialNumber = serialNumber
                isSerialSet = true
            } else {
                isSerialSet = false
            }
        }
    }
}
