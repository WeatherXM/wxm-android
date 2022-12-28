package com.weatherxm.ui.home

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.data.DataError
import com.weatherxm.usecases.UserUseCase
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class HomeViewModel : ViewModel(), KoinComponent {

    private val userUseCase: UserUseCase by inject()

    // Needed for passing info to show the wallet missing warning card and badges
    private val onWalletMissing = MutableLiveData(false)

    fun onWalletMissing() = onWalletMissing

    fun getWalletMissing() {
        viewModelScope.launch {
            userUseCase.getWalletAddress()
                .map {
                    onWalletMissing.postValue(it.isEmpty())
                }.mapLeft {
                    onWalletMissing.postValue(it is DataError.NoWalletAddressError)
                }
        }
    }

    fun setWalletNotMissing() {
        onWalletMissing.postValue(false)
    }
}
