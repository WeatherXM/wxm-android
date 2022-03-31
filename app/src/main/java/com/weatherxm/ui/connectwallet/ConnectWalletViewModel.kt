package com.weatherxm.ui.connectwallet

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import arrow.core.Validated
import arrow.core.valueOr
import com.weatherxm.R
import com.weatherxm.data.ApiError.UserError.WalletError.InvalidWalletAddress
import com.weatherxm.data.Failure
import com.weatherxm.data.Failure.NetworkError
import com.weatherxm.data.Resource
import com.weatherxm.usecases.ConnectWalletUseCase
import com.weatherxm.util.ResourcesHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ConnectWalletViewModel : ViewModel(), KoinComponent {

    companion object {
        const val ETH_ADDR_PREFIX: String = "0x"
    }

    private val connectWalletUseCase: ConnectWalletUseCase by inject()
    private val resHelper: ResourcesHelper by inject()

    private var currentAddress = MutableLiveData<String?>(null)
    fun currentAddress() = currentAddress.apply {
        // Get initial value from repository
        CoroutineScope(Dispatchers.IO).launch {
            connectWalletUseCase.getWalletAddress()
                .map { postValue(it) }
        }
    }

    private var newAddress = MutableLiveData<String?>(null)
    fun newAddress() = newAddress

    private val isAddressSaved = MutableLiveData<Resource<String>>()
    fun isAddressSaved() = isAddressSaved

    fun saveAddress(address: String, termsChecked: Boolean, ownershipChecked: Boolean) {
        isAddressSaved.postValue(Resource.loading())
        if (termsChecked && ownershipChecked) {
            CoroutineScope(Dispatchers.IO).launch {
                connectWalletUseCase.setWalletAddress(address)
                    .mapLeft {
                        handleFailure(it)
                    }.map {
                        isAddressSaved.postValue(
                            Resource.success(resHelper.getString(R.string.address_saved))
                        )
                        currentAddress.postValue(address)
                    }
            }
        } else if (ownershipChecked) {
            isAddressSaved.postValue(
                Resource.error(
                    resHelper.getString(R.string.warn_connect_wallet_terms_not_accepted)
                )
            )
        } else {
            isAddressSaved.postValue(
                Resource.error(
                    resHelper.getString(R.string.warn_connect_wallet_access_not_acknowledged)
                )
            )
        }
    }

    private fun handleFailure(failure: Failure) {
        isAddressSaved.postValue(
            Resource.error(
                resHelper.getString(
                    when (failure) {
                        is InvalidWalletAddress -> R.string.error_connect_wallet_invalid_address
                        is NetworkError -> R.string.error_network
                        else -> R.string.error_unknown
                    }
                )
            )
        )
    }

    /*
    * Custom fix because scanning the address in Metamask adds the "ethereum:" prefix
    * Also fix if the QR scanned is not an ETH address (or is null) to return null and not proceed
    */
    fun onScanAddress(scannedAddress: String?) {
        if (scannedAddress.isNullOrEmpty() || !scannedAddress.contains(ETH_ADDR_PREFIX)) {
            return
        }
        newAddress.postValue(sanitize(scannedAddress))
    }

    private fun sanitize(address: String): String {
        return Validated.Valid(address)
            .map { it.substring(it.indexOf(ETH_ADDR_PREFIX)) }
            .valueOr { "" }
    }
}
