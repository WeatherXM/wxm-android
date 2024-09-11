package com.weatherxm.ui.home.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.ApiError
import com.weatherxm.data.Resource
import com.weatherxm.data.SingleLiveEvent
import com.weatherxm.data.Survey
import com.weatherxm.data.User
import com.weatherxm.ui.common.UIWalletRewards
import com.weatherxm.usecases.RemoteBannersUseCase
import com.weatherxm.usecases.UserUseCase
import com.weatherxm.util.Failure.getDefaultMessage
import kotlinx.coroutines.launch
import timber.log.Timber

class ProfileViewModel(
    private val useCase: UserUseCase,
    private val remoteBannersUseCase: RemoteBannersUseCase,
    private val analytics: AnalyticsWrapper
) : ViewModel() {
    private var currentWalletRewards: UIWalletRewards? = null

    private val onUser = MutableLiveData<Resource<User>>()
    private val onSurvey = SingleLiveEvent<Survey>()
    private val onWalletRewards = MutableLiveData<Resource<UIWalletRewards>>().apply {
        value = Resource.loading()
    }

    fun onUser(): LiveData<Resource<User>> = onUser
    fun onSurvey(): LiveData<Survey> = onSurvey
    fun onWalletRewards(): LiveData<Resource<UIWalletRewards>> = onWalletRewards

    fun fetchUser(forceRefresh: Boolean = false) {
        onUser.postValue(Resource.loading())
        viewModelScope.launch {
            useCase.getUser(forceRefresh)
                .onRight {
                    onUser.postValue(Resource.success(it))
                    fetchWalletRewards(it.wallet?.address)
                }
                .onLeft {
                    analytics.trackEventFailure(it.code)
                    onUser.postValue(
                        Resource.error(it.getDefaultMessage(R.string.error_reach_out_short))
                    )
                }
        }
    }

    fun getSurvey() {
        remoteBannersUseCase.getSurvey().apply {
            if (this != null) {
                onSurvey.postValue(this)
            }
        }
    }

    fun dismissSurvey(surveyId: String) {
        remoteBannersUseCase.dismissSurvey(surveyId)
    }

    fun onClaimedResult(amountClaimed: Double) {
        currentWalletRewards?.apply {
            allocated -= amountClaimed
            totalClaimed += amountClaimed
        }
        onWalletRewards.postValue(Resource.success(currentWalletRewards))
    }

    private fun fetchWalletRewards(walletAddress: String?) {
        viewModelScope.launch {
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
