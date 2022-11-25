package com.weatherxm.ui.claimdevice.result

import androidx.lifecycle.ViewModel
import com.weatherxm.data.Device
import org.koin.core.component.KoinComponent

class ClaimResultViewModel : ViewModel(), KoinComponent {
    private var claimedDevice: Device? = null

    fun setClaimedDevice(device: Device?) {
        claimedDevice = device
    }

    fun getClaimedDevice(): Device? {
        return claimedDevice
    }
}
