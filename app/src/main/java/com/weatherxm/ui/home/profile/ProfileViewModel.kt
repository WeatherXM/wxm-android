package com.weatherxm.ui.home.profile

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.data.Resource
import com.weatherxm.data.User
import com.weatherxm.usecases.UserUseCase
import com.weatherxm.util.UIErrors.getDefaultMessage
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class ProfileViewModel : ViewModel(), KoinComponent {

    private val userUseCase: UserUseCase by inject()

    private val user = MutableLiveData<Resource<User>>()
    fun user() = user

    private val wallet = MutableLiveData<String?>()
    fun wallet() = wallet

    fun refreshWallet() {
        viewModelScope.launch {
            fetchWallet()
        }
    }

    private suspend fun fetchUser() {
        userUseCase.getUser()
            .map {
                user.postValue(Resource.success(it))
            }.mapLeft {
                user.postValue(Resource.error(it.getDefaultMessage()))
            }
    }

    private suspend fun fetchWallet() {
        Timber.d("Getting wallet in the background")
        userUseCase.getWalletAddress()
            .map { wallet.postValue(it) }
            .mapLeft { wallet.postValue(null) }
    }

    init {
        viewModelScope.launch {
            fetchUser()
            fetchWallet()
        }
    }
}
