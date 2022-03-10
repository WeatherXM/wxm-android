package com.weatherxm.ui.home.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.weatherxm.R
import com.weatherxm.data.Failure
import com.weatherxm.data.Resource
import com.weatherxm.ui.ProfileInfo
import com.weatherxm.usecases.UserUseCase
import com.weatherxm.util.ResourcesHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class ProfileViewModel : ViewModel(), KoinComponent {

    private val userUseCase: UserUseCase by inject()
    private val resHelper: ResourcesHelper by inject()

    private val profileInfo = MutableLiveData<Resource<ProfileInfo>>().apply {
        value = Resource.loading()
    }

    fun profileInfo(): LiveData<Resource<ProfileInfo>> = profileInfo

    private val hasWallet = MutableLiveData(true)
    fun hasWallet(): LiveData<Boolean> = hasWallet

    /*
     * We use this function to fetch the user and cache the result for future use
     * as also to use the hasWallet LiveData to push some needed data
     * to the HomeActivity that called this fetch function
     */
    fun fetchUser() {
        CoroutineScope(Dispatchers.IO).launch {
            userUseCase.getUser()
                .map { user ->
                    Timber.d("Got user: $user")
                    hasWallet.postValue(!user.wallet?.address.isNullOrEmpty())
                }
                .mapLeft {
                    handleFailure(it)
                }
        }
    }

    /*
     * We use this function to fetch the user and cache the result for future use
     * as also to use the hasWallet LiveData to push some needed data
     * to the HomeActivity that called this fetch function
     */
    fun fetchProfileInfo() {
        CoroutineScope(Dispatchers.IO).launch {
            userUseCase.getProfileInfo()
                .map {
                    Timber.d("Got profile info: $it")
                    profileInfo.postValue(Resource.success(it))
                }
                .mapLeft {
                    handleFailure(it)
                }
        }
    }

    fun getWalletAddressFromCache(): String? {
        return userUseCase.getWalletAddressFromCache()
    }

    fun walletConnected() {
        hasWallet.postValue(true)
    }

    private fun handleFailure(failure: Failure) {
        profileInfo.postValue(
            Resource.error(
                resHelper.getString(
                    when (failure) {
                        is Failure.NetworkError -> R.string.network_error
                        else -> R.string.unknown_error
                    }
                )
            )
        )
    }
}
