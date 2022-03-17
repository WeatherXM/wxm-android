package com.weatherxm.ui.home.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.weatherxm.R
import com.weatherxm.data.Failure
import com.weatherxm.data.Resource
import com.weatherxm.data.User
import com.weatherxm.data.repository.UserRepository
import com.weatherxm.util.ResourcesHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class ProfileViewModel : ViewModel(), KoinComponent {

    private val userRepository: UserRepository by inject()
    private val resHelper: ResourcesHelper by inject()

    private val user = MutableLiveData<Resource<User>>().apply {
        value = Resource.loading()
    }

    fun user(): LiveData<Resource<User>> = user

    private val hasWallet = MutableLiveData(true)
    fun hasWallet(): LiveData<Boolean> = hasWallet

    fun fetch() {
        CoroutineScope(Dispatchers.IO).launch {
            userRepository.getUser()
                .map { user ->
                    Timber.d("Got user: $user")
                    this@ProfileViewModel.user.postValue(Resource.success(user))
                }
                .mapLeft {
                    handleFailure(it)
                }
        }
    }

    fun getWallet() {
        CoroutineScope(Dispatchers.IO).launch {
            userRepository.getUser()
                .map { user ->
                    Timber.d("Has wallet: ${user.wallet?.address.isNullOrEmpty()}")
                    hasWallet.postValue(!user.wallet?.address.isNullOrEmpty())
                }
        }
    }

    fun walletConnected() {
        hasWallet.postValue(true)
    }

    private fun handleFailure(failure: Failure) {
        user.postValue(
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
