package com.weatherxm.ui.claimdevice.m5.verify

import androidx.lifecycle.ViewModel
import com.weatherxm.ui.common.empty
import com.weatherxm.util.Validator.validateSerialNumber

class ClaimM5VerifyViewModel : ViewModel() {
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
