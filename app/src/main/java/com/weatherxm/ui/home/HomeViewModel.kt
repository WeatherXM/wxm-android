package com.weatherxm.ui.home

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.koin.core.component.KoinComponent

class HomeViewModel : ViewModel(), KoinComponent {
    private val onClaimM5 = MutableLiveData(false)
    fun onClaimM5() = onClaimM5

    private val onClaimHelium = MutableLiveData(false)
    fun onClaimHelium() = onClaimHelium

    fun claimHelium() {
        onClaimHelium.postValue(true)
    }

    fun claimM5() {
        onClaimM5.postValue(true)
    }
}
