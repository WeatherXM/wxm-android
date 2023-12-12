package com.weatherxm.ui.home

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.data.DataError
import com.weatherxm.data.SingleLiveEvent
import com.weatherxm.ui.common.DeviceRelation
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.usecases.UserUseCase
import com.weatherxm.util.Analytics
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class HomeViewModel : ViewModel(), KoinComponent {

    private val userUseCase: UserUseCase by inject()
    private val analytics: Analytics by inject()

    private var hasDevices: Boolean? = null

    // Needed for passing info to show the wallet missing warning card and badges
    private val onWalletMissingWarning = MutableLiveData(false)
    private val onWalletMissing = MutableLiveData(false)
    private val onOpenExplorer = SingleLiveEvent<Boolean>()

    fun onWalletMissingWarning() = onWalletMissingWarning
    fun onWalletMissing() = onWalletMissing
    fun onOpenExplorer() = onOpenExplorer

    fun openExplorer() {
        onOpenExplorer.postValue(true)
    }

    fun hasDevices() = hasDevices

    fun getWalletMissing(devices: List<UIDevice>?) {
        if (devices?.firstOrNull { it.relation == DeviceRelation.OWNED } == null) {
            hasDevices = false
            return
        }
       hasDevices = true
        viewModelScope.launch {
            onWalletMissingWarning.postValue(userUseCase.shouldShowWalletMissingWarning())
            userUseCase.getWalletAddress()
                .onRight {
                    onWalletMissing.postValue(it.isEmpty())
                }
                .onLeft {
                    if (it !is DataError.NoWalletAddressError) {
                        analytics.trackEventFailure(it.code)
                    }
                    onWalletMissing.postValue(it is DataError.NoWalletAddressError)
                }
        }
    }

    fun setHasDevices(hasDevices: Boolean) {
        this.hasDevices = hasDevices
    }

    fun setWalletWarningDismissTimestamp() {
        userUseCase.setWalletWarningDismissTimestamp()
    }

    fun setWalletNotMissing() {
        onWalletMissingWarning.postValue(false)
        onWalletMissing.postValue(false)
    }
}
