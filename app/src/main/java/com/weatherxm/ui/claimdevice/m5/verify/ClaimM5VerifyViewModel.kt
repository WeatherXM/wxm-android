package com.weatherxm.ui.claimdevice.m5.verify

import androidx.lifecycle.ViewModel
import com.weatherxm.util.Validator
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ClaimM5VerifyViewModel : ViewModel(), KoinComponent {
    private val validator: Validator by inject()
    private var currentSerialNumber: String = ""

    fun setSerialNumber(serial: String) {
        currentSerialNumber = serial
    }

    fun getSerialNumber(): String {
        return currentSerialNumber
    }

    fun validateSerial(serialNumber: String): Boolean {
        return validator.validateSerialNumber(serialNumber)
    }
}