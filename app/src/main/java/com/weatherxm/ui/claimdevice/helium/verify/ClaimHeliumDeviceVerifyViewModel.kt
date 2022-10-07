package com.weatherxm.ui.claimdevice.helium.verify

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.weatherxm.util.ResourcesHelper
import com.weatherxm.util.Validator
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ClaimHeliumDeviceVerifyViewModel : ViewModel(), KoinComponent {
    private val validator: Validator by inject()
    private val resHelper: ResourcesHelper by inject()

    private val onDevKeyError = MutableLiveData(false)
    fun onDevKeyError() = onDevKeyError

    private val onDevEUIError = MutableLiveData(false)
    fun onDevEUIError() = onDevEUIError

    private val onVerifyError = MutableLiveData(false)
    fun onVerifyError() = onVerifyError

    fun getEUIFromScanner(result: String?): String {
        return result?.take(16) ?: ""
    }

    fun getKeyFromScanner(result: String?): String {
        return result?.substring(16..31) ?: ""
    }

    fun checkAndVerify(devEUI: String, devKey: String) {
        val validDevEUI = validator.validateDevEUI(devEUI)
        val validDevKey = validator.validateDevKey(devKey)

        onDevEUIError.postValue(!validDevEUI)
        onDevKeyError.postValue(!validDevKey)

        if (validDevEUI && validDevKey) {
            // TODO: Verified. Continue with API call.
        }
    }
}
