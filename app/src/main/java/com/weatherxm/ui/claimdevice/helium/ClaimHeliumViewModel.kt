package com.weatherxm.ui.claimdevice.helium

import android.location.Location
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.data.Resource
import com.weatherxm.ui.claimdevice.result.ClaimResult
import com.weatherxm.usecases.ClaimDeviceUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ClaimHeliumViewModel : ViewModel(), KoinComponent {
    private val claimDeviceUseCase: ClaimDeviceUseCase by inject()

    private val onCancel = MutableLiveData(false)
    private val onNext = MutableLiveData(false)
    private val onBackToLocation = MutableLiveData(false)
    private val onClaimResult = MutableLiveData<Resource<ClaimResult>>().apply {
        value = Resource.loading()
    }
    private val onClaimManually = MutableLiveData(false)

    fun onCancel() = onCancel
    fun onNext() = onNext
    fun onBackToLocation() = onBackToLocation
    fun onClaimResult() = onClaimResult
    fun onClaimManually() = onClaimManually

    private var userEmail: String? = null
    private var isManual = false

    fun cancel() {
        onCancel.postValue(true)
    }

    fun next() {
        onNext.postValue(true)
    }

    fun backToLocation() {
        onBackToLocation.postValue(true)
    }

    fun setManual(isManual: Boolean?) {
        this.isManual = isManual == true
    }

    fun isManualClaiming(): Boolean {
        return isManual
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

    fun claimManually() {
        onClaimManually.postValue(true)
    }

    fun claimDevice(devEUI: String, devKey: String, location: Location) {
        onClaimResult.postValue(Resource.loading())
        viewModelScope.launch {
            // TODO: API call
            delay(3000L)
            onClaimResult.postValue(Resource.success(null))
            delay(3000L)
            onClaimResult.postValue(
                Resource.error("Oopsie", ClaimResult(errorCode = "Error code to include in email"))
            )
        }
    }
}
