package com.weatherxm.ui.home.profile

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
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

    private fun fetchUser() {
        viewModelScope.launch {
            userUseCase.getUser()
                .map {
                    user.postValue(Resource.success(it))
                }.mapLeft {
                    user.postValue(
                        Resource.error(it.getDefaultMessage(R.string.error_reach_out_short))
                    )
                }
        }
    }

    fun fetchWallet() {
        viewModelScope.launch {
            Timber.d("Getting wallet in the background")
            userUseCase.getWalletAddress()
                .map { wallet.postValue(it) }
                .mapLeft { wallet.postValue(null) }
        }
    }

    init {
        fetchUser()
        fetchWallet()
    }
}
