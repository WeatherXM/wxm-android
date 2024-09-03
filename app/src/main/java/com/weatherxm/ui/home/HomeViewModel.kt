package com.weatherxm.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.SingleLiveEvent
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.WalletInfo
import com.weatherxm.ui.common.empty
import com.weatherxm.usecases.UserUseCase
import kotlinx.coroutines.launch

class HomeViewModel(
    private val userUseCase: UserUseCase,
    private val analytics: AnalyticsWrapper
) : ViewModel() {

    private var hasDevices: Boolean? = null

    // Needed for passing info to show the wallet missing warning card and badges
    private val onWalletInfo = MutableLiveData<WalletInfo>()
    private val onOpenExplorer = SingleLiveEvent<Boolean>()

    fun onWalletInfo(): LiveData<WalletInfo> = onWalletInfo
    fun onOpenExplorer() = onOpenExplorer

    fun openExplorer() {
        onOpenExplorer.postValue(true)
    }

    fun hasDevices() = hasDevices

    fun getWalletInfo(devices: List<UIDevice>?) {
        hasDevices = devices?.firstOrNull { it.isOwned() } != null
        viewModelScope.launch {
            userUseCase.getWalletAddress().onRight {
                onWalletInfo.postValue(
                    WalletInfo(
                        it,
                        showMissingBadge = it.isEmpty(),
                        showMissingWarning = userUseCase.shouldShowWalletMissingWarning(it)
                    )
                )
            }.onLeft {
                analytics.trackEventFailure(it.code)
                onWalletInfo.postValue(
                    WalletInfo(
                        String.empty(),
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

    fun setWalletNotMissing(walletAddress: String?) {
        onWalletInfo.postValue(
            WalletInfo(
                walletAddress ?: String.empty(),
                showMissingBadge = false,
                showMissingWarning = false
            )
        )
    }
}
