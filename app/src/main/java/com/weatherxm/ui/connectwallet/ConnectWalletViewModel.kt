package com.weatherxm.ui.connectwallet

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.data.ApiError.UserError.WalletError.InvalidWalletAddress
import com.weatherxm.data.Failure
import com.weatherxm.data.Resource
import com.weatherxm.ui.common.empty
import com.weatherxm.usecases.ConnectWalletUseCase
import com.weatherxm.analytics.AnalyticsImpl
import com.weatherxm.util.Failure.getDefaultMessageResId
import com.weatherxm.util.Resources
import kotlinx.coroutines.launch

class ConnectWalletViewModel(
    private val connectWalletUseCase: ConnectWalletUseCase,
    private val resources: Resources,
    private val analytics: AnalyticsImpl
) : ViewModel() {

    companion object {
        const val ETH_ADDR_PREFIX: String = "0x"
        const val LAST_CHARS_TO_SHOW_AS_CONFIRM = 5
    }

    private var currentAddress = MutableLiveData(String.empty())
    fun currentAddress() = currentAddress

    private val isAddressSaved = MutableLiveData<Resource<String>>()
    fun isAddressSaved() = isAddressSaved

    fun saveAddress(address: String) {
        isAddressSaved.postValue(Resource.loading())
        viewModelScope.launch {
            connectWalletUseCase.setWalletAddress(address)
                .mapLeft {
                    analytics.trackEventFailure(it.code)
                    handleFailure(it)
                }
                .map {
                    isAddressSaved.postValue(
                        Resource.success(resources.getString(R.string.address_saved))
                    )
                    currentAddress.postValue(address)
                }
        }
    }

    fun getLastPartOfAddress(address: String): String {
        return address.substring(address.length - LAST_CHARS_TO_SHOW_AS_CONFIRM)
    }

    private fun handleFailure(failure: Failure) {
        isAddressSaved.postValue(
            Resource.error(
                resources.getString(
                    if (failure is InvalidWalletAddress) {
                        R.string.error_connect_wallet_invalid_address
                    } else {
                        failure.getDefaultMessageResId(R.string.error_reach_out_short)
                    }
                )
            )
        )
    }

    /*
    * Custom fix because scanning the address in Metamask adds the "ethereum:" prefix
    * Also fix if the QR scanned is not an ETH address (or is null) to return null and not proceed
    */
    fun onScanAddress(scannedAddress: String?): String? {
        return if (scannedAddress.isNullOrEmpty() || !scannedAddress.contains(ETH_ADDR_PREFIX)) {
            null
        } else {
            sanitize(scannedAddress)
        }
    }

    @Suppress("MagicNumber")
    private fun sanitize(address: String): String {
        return address.substring(address.indexOf(ETH_ADDR_PREFIX)).slice(0..41)
    }

    init {
        // Get initial value from repository
        viewModelScope.launch {
            connectWalletUseCase.getWalletAddress().map { currentAddress.postValue(it) }
        }
    }
}
