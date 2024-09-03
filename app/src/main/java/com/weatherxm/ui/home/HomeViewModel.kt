package com.weatherxm.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.SingleLiveEvent
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.WalletWarnings
import com.weatherxm.usecases.UserUseCase
import kotlinx.coroutines.launch

class HomeViewModel(
    private val userUseCase: UserUseCase,
    private val analytics: AnalyticsWrapper
) : ViewModel() {

    private var hasDevices: Boolean? = null

    // Needed for passing info to show the wallet missing warning card and badges
    private val onWalletWarnings = MutableLiveData<WalletWarnings>()
    private val onOpenExplorer = SingleLiveEvent<Boolean>()

    fun onWalletWarnings(): LiveData<WalletWarnings> = onWalletWarnings
    fun onOpenExplorer() = onOpenExplorer

    fun openExplorer() {
        onOpenExplorer.postValue(true)
    }

    fun hasDevices() = hasDevices

    fun getWalletWarnings(devices: List<UIDevice>?) {
        hasDevices = devices?.firstOrNull { it.isOwned() } != null
        viewModelScope.launch {
            userUseCase.getWalletAddress().onRight {
                onWalletWarnings.postValue(
                    WalletWarnings(
                        showMissingBadge = it.isEmpty(),
                        showMissingWarning = userUseCase.shouldShowWalletMissingWarning(it)
                    )
                )
            }.onLeft {
                analytics.trackEventFailure(it.code)
                onWalletWarnings.postValue(
                    WalletWarnings(
                        showMissingBadge = false,
                        showMissingWarning = false
                    )
                )
            }
        }
    }

    fun setWalletWarningDismissTimestamp() {
        userUseCase.setWalletWarningDismissTimestamp()
    }

    fun setWalletNotMissing() {
        onWalletWarnings.postValue(
            WalletWarnings(
                showMissingBadge = false,
                showMissingWarning = false
            )
        )
    }
}
