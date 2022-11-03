package com.weatherxm.ui.home

import androidx.lifecycle.ViewModel
import com.weatherxm.data.SingleLiveEvent
import org.koin.core.component.KoinComponent

class HomeViewModel : ViewModel(), KoinComponent {
    /*
     * We use SingleLiveEvent because MutableLiveData persists and re-posts the value
     * to the observers on configuration change (like a theme change) and the effects of the
     * observers happen again (like restarting the claiming activity).
     */
    private val onClaimM5Manually = SingleLiveEvent<Boolean>()
    private val onClaimHelium = SingleLiveEvent<Boolean>()

    fun onClaimM5Manually() = onClaimM5Manually
    fun onClaimHelium() = onClaimHelium

    fun claimHelium() {
        onClaimHelium.postValue(true)
    }

    fun claimM5Manually() {
        onClaimM5Manually.postValue(true)
    }
}
