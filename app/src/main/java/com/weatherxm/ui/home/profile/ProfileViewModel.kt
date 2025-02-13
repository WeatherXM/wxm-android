package com.weatherxm.ui.home.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.ApiError
import com.weatherxm.data.models.User
import com.weatherxm.ui.common.Resource
import com.weatherxm.ui.common.UIWalletRewards
import com.weatherxm.usecases.UserUseCase
import com.weatherxm.util.Failure.getDefaultMessage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import timber.log.Timber

class ProfileViewModel(
    private val useCase: UserUseCase,
    private val analytics: AnalyticsWrapper,
    private val dispatcher: CoroutineDispatcher
) : ViewModel() {
    private var currentWalletRewards: UIWalletRewards? = null

    private val onUser = MutableLiveData<Resource<User>>()
    private val onWalletRewards = MutableLiveData<Resource<UIWalletRewards>>()

    fun onUser(): LiveData<Resource<User>> = onUser
    fun onWalletRewards(): LiveData<Resource<UIWalletRewards>> = onWalletRewards

    fun fetchUser(forceRefresh: Boolean = false) {
        onUser.postValue(Resource.loading())
        viewModelScope.launch(dispatcher) {
            useCase.getUser(forceRefresh).onRight {
                onUser.postValue(Resource.success(it))
                fetchWalletRewards(it.wallet?.address)
            }.onLeft {
                analytics.trackEventFailure(it.code)
                onUser.postValue(
                    Resource.error(it.getDefaultMessage(R.string.error_reach_out_short))
                )
            }
        }
    }

    fun onClaimedResult(amountClaimed: Double) {
        currentWalletRewards?.apply {
            allocated -= amountClaimed
            totalClaimed += amountClaimed
        }
        onWalletRewards.postValue(Resource.success(currentWalletRewards))
    }

    private fun fetchWalletRewards(walletAddress: String?) {
        viewModelScope.launch(dispatcher) {
            onWalletRewards.postValue(Resource.loading())
            useCase.getWalletRewards(walletAddress).onRight {
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
