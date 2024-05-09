package com.weatherxm.ui.claimdevice.wifi.verify

import androidx.lifecycle.ViewModel
import com.weatherxm.ui.common.empty
import com.weatherxm.util.Validator.validateSerialNumber

class ClaimWifiVerifyViewModel : ViewModel() {
    private var currentSerialNumber: String = String.empty()

    fun setSerialNumber(serial: String) {
        currentSerialNumber = serial
    }

    fun getSerialNumber(): String {
        return currentSerialNumber
    }

    fun validateSerial(serialNumber: String): Boolean {
        return validateSerialNumber(serialNumber)
    }
}
