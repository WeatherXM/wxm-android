package com.weatherxm.ui.home

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.koin.core.component.KoinComponent

class HomeViewModel : ViewModel(), KoinComponent {
    private val onClaimM5Manually = MutableLiveData(false)
    fun onClaimM5Manually() = onClaimM5Manually

    private val onClaimHelium = MutableLiveData(false)
    fun onClaimHelium() = onClaimHelium

    fun claimHelium() {
        onClaimHelium.postValue(true)
    }

    fun claimM5Manually() {
        onClaimM5Manually.postValue(true)
    }
}
