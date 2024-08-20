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
import com.weatherxm.usecases.SurveyUseCase
import com.weatherxm.usecases.UserUseCase
import com.weatherxm.util.Failure.getDefaultMessage
import kotlinx.coroutines.launch
import timber.log.Timber

class ProfileViewModel(
    private val useCase: UserUseCase,
    private val surveyUseCase: SurveyUseCase,
    private val analytics: AnalyticsWrapper
) : ViewModel() {

    private val onLoading = MutableLiveData<Boolean>()
    private val onUser = MutableLiveData<Resource<User>>()
    private val onWalletRewards = MutableLiveData<Resource<UIWalletRewards>>()
    private val onSurvey = SingleLiveEvent<Survey>()

    fun onLoading(): LiveData<Boolean> = onLoading
    fun onUser(): LiveData<Resource<User>> = onUser
    fun onWalletRewards(): LiveData<Resource<UIWalletRewards>> = onWalletRewards
    fun onSurvey(): LiveData<Survey> = onSurvey

    private var currentWalletRewards: UIWalletRewards? = null

    fun onClaimedResult(amountClaimed: Double) {
        currentWalletRewards?.apply {
            allocated -= amountClaimed
            totalClaimed += amountClaimed
        }
        onWalletRewards.postValue(Resource.success(currentWalletRewards))
    }

    fun fetchUser(forceRefresh: Boolean = false) {
        onLoading.postValue(true)
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
                    onLoading.postValue(false)
                }
        }
    }

    fun getSurvey() {
        surveyUseCase.getSurvey().apply {
            if (this != null) {
                onSurvey.postValue(this)
            }
        }
    }

    fun dismissSurvey(surveyId: String) {
        surveyUseCase.dismissSurvey(surveyId)
    }

    private fun fetchWalletRewards(walletAddress: String?) {
        viewModelScope.launch {
            onLoading.postValue(true)
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
            onLoading.postValue(false)
        }
    }
}
