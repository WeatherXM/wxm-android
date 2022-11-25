package com.weatherxm.ui.home

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.data.DataError
import com.weatherxm.data.Device
import com.weatherxm.data.SingleLiveEvent
import com.weatherxm.usecases.UserUseCase
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class HomeViewModel : ViewModel(), KoinComponent {

    private val userUseCase: UserUseCase by inject()

    // Needed for passing info to show the wallet missing warning card and badges
    private val onWalletMissing = MutableLiveData(false)

    /*
     * We use SingleLiveEvent because MutableLiveData persists and re-posts the value
     * to the observers on configuration change (like a theme change) and the effects of the
     * observers happen again (like restarting the claiming activity).
     */
    private val onClaimM5Manually = SingleLiveEvent<Boolean>()
    private val onClaimHelium = SingleLiveEvent<Boolean>()

    /**
     * Needed for passing info to the fragment to notify it
     * to start a new activity (UserDeviceActivity) for the claimed device
     */
    private val onDeviceClaimed = SingleLiveEvent<Device>()

    fun onWalletMissing() = onWalletMissing
    fun onClaimM5Manually() = onClaimM5Manually
    fun onClaimHelium() = onClaimHelium
    fun onDeviceClaimed() = onDeviceClaimed

    fun getWalletMissing() {
        viewModelScope.launch {
            userUseCase.getWalletAddress()
                .map {
                    onWalletMissing.postValue(it.isEmpty())
                }.mapLeft {
                    onWalletMissing.postValue(it is DataError.NoWalletAddressError)
                }
        }
    }

    fun setWalletNotMissing() {
        onWalletMissing.postValue(false)
    }

    fun claimHelium() {
        onClaimHelium.postValue(true)
    }

    fun claimM5Manually() {
        onClaimM5Manually.postValue(true)
    }

    fun deviceClaimed(device: Device) {
        onDeviceClaimed.postValue(device)
    }
}
