package com.weatherxm.ui.connectwallet

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.weatherxm.R
import com.weatherxm.data.Failure
import com.weatherxm.data.Resource
import com.weatherxm.data.ServerError
import com.weatherxm.data.repository.UserRepository
import com.weatherxm.util.ResourcesHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

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
                    Timber.d("Got error: $it")
                    when (it) {
                        is Failure.NetworkError -> isAddressSaved.postValue(
                            Resource.error(
                                resHelper.getString(R.string.address_save_network_error)
                            )
                        )
                        is ServerError -> isAddressSaved.postValue(
                            Resource.error(
                                resHelper.getString(R.string.address_save_server_error)
                            )
                        )
                        is Failure.UnknownError -> isAddressSaved.postValue(
                            Resource.error(
                                resHelper.getString(R.string.unknown_error)
                            )
                        )
                    }
                }.map {
                    isAddressSaved.postValue(
                        Resource.success(resHelper.getString(R.string.address_saved))
                    )
                    setCurrentAddress(address)
                }
        }
    }

    fun setCurrentAddress(address: String?) {
        currentAddress.postValue(address)
    }
}
