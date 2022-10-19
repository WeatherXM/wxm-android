package com.weatherxm.ui.claimdevice.helium

import android.location.Location
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.data.Resource
import com.weatherxm.usecases.ClaimDeviceUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ClaimHeliumViewModel : ViewModel(), KoinComponent {
    private val claimDeviceUseCase: ClaimDeviceUseCase by inject()

    private val onCancel = MutableLiveData(false)
    private val onNext = MutableLiveData(false)
    private val onClaimResult = MutableLiveData<Resource<String>>().apply {
        value = Resource.loading()
    }

    private var userEmail: String? = null

    fun onCancel() = onCancel
    fun onNext() = onNext
    fun onClaimResult() = onClaimResult

    fun cancel() {
        onCancel.postValue(true)
    }

    fun next() {
        onNext.postValue(true)
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

    fun claimDevice(devEUI: String, devKey: String, location: Location) {
        onClaimResult.postValue(Resource.loading())
        viewModelScope.launch {
            // TODO: API call
            delay(3000L)
            onClaimResult.postValue(Resource.success("NEW WEATHER STATION"))
            delay(3000L)
            onClaimResult.postValue(Resource.error("Oopsie"))
        }
    }
}
