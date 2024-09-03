package com.weatherxm.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.ApiError
import com.weatherxm.data.Resource
import com.weatherxm.data.SingleLiveEvent
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.UIWalletRewards
import com.weatherxm.ui.common.WalletInfo
import com.weatherxm.ui.common.empty
import com.weatherxm.usecases.UserUseCase
import com.weatherxm.util.Failure.getDefaultMessage
import kotlinx.coroutines.launch
import timber.log.Timber

class HomeViewModel(
    private val userUseCase: UserUseCase,
    private val analytics: AnalyticsWrapper
) : ViewModel() {

    private var hasDevices: Boolean? = null
    private var currentWalletRewards: UIWalletRewards? = null

    // Needed for passing info to show the wallet missing warning card and badges
    private val onWalletInfo = MutableLiveData<WalletInfo>()
    private val onOpenExplorer = SingleLiveEvent<Boolean>()
    private val onWalletRewards = MutableLiveData<Resource<UIWalletRewards>>().apply {
        value = Resource.loading()
    }

    fun onWalletInfo(): LiveData<WalletInfo> = onWalletInfo
    fun onOpenExplorer() = onOpenExplorer
    fun onWalletRewards(): LiveData<Resource<UIWalletRewards>> = onWalletRewards

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

    fun onClaimedResult(amountClaimed: Double) {
        currentWalletRewards?.apply {
            allocated -= amountClaimed
            totalClaimed += amountClaimed
        }
        onWalletRewards.postValue(Resource.success(currentWalletRewards))
    }

    fun fetchWalletRewards(walletAddress: String?) {
        viewModelScope.launch {
            onWalletRewards.postValue(Resource.loading())
            userUseCase.getWalletRewards(walletAddress).onRight {
                Timber.d("Got Wallet Rewards: $it")
                currentWalletRewards = it
                onWalletRewards.postValue(Resource.success(it))
            }.onLeft {
                analytics.trackEventFailure(it.code)
                Timber.e("[FETCH WALLET REWARDS] Error $it")
                if (it is ApiError.UserError.WalletError.WalletAddressNotFound) {
                    onWalletRewards.postValue(Resource.success(UIWalletRewards.empty()))
                } else {
                    onWalletRewards.postValue(
                        Resource.error(it.getDefaultMessage(R.string.error_reach_out_short))
                    )
                }
            }
        }
    }
}
