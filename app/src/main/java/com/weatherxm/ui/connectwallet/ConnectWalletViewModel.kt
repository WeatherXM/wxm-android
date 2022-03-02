package com.weatherxm.ui.connectwallet

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.weatherxm.R
import com.weatherxm.data.ApiError.UserError.WalletError.InvalidWalletAddress
import com.weatherxm.data.Failure
import com.weatherxm.data.Failure.NetworkError
import com.weatherxm.data.Resource
import com.weatherxm.data.repository.UserRepository
import com.weatherxm.util.ResourcesHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ConnectWalletViewModel : ViewModel(), KoinComponent {

    private val isAddressSaved = MutableLiveData<Resource<String>>()
    fun isAddressSaved() = isAddressSaved

    private val userRepository: UserRepository by inject()
    private val resHelper: ResourcesHelper by inject()

    private var currentAddress = MutableLiveData<String?>(null)
    fun currentAddress() = currentAddress

    fun saveAddress(address: String) {
        isAddressSaved.postValue(Resource.loading())
        CoroutineScope(Dispatchers.IO).launch {
            userRepository.saveAddress(address)
                .mapLeft {
                    handleFailure(it)
                }.map {
                    isAddressSaved.postValue(
                        Resource.success(resHelper.getString(R.string.address_saved))
                    )
                    setCurrentAddress(address)
                }
        }
    }

    private fun handleFailure(failure: Failure) {
        isAddressSaved.postValue(
            Resource.error(
                resHelper.getString(
                    when (failure) {
                        is InvalidWalletAddress -> R.string.connect_wallet_invalid_address
                        is NetworkError -> R.string.network_error
                        else -> R.string.unknown_error
                    }
                )
            )
        )
    }

    fun setCurrentAddress(address: String?) {
        currentAddress.postValue(address)
    }

    // Custom fix because scanning the address in Metamask adds the "ethereum:" prefix
    fun fixQrAddressScanned(scannedAddress: String): String {
        return if (scannedAddress.startsWith("0x")) {
            scannedAddress
        } else {
            scannedAddress.substring(scannedAddress.indexOf("0x"))
        }
    }
}
