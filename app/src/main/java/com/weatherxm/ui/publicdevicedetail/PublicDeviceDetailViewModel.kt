package com.weatherxm.ui.publicdevicedetail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.data.Resource
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.explorer.UIHex
import com.weatherxm.usecases.ExplorerUseCase
import com.weatherxm.util.Analytics
import com.weatherxm.util.ResourcesHelper
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class PublicDeviceDetailViewModel : ViewModel(), KoinComponent {
    private val resHelper: ResourcesHelper by inject()
    private val explorerUseCase: ExplorerUseCase by inject()
    private val analytics: Analytics by inject()
    private val onPublicDevice = MutableLiveData<Resource<UIDevice>>()
    private val address = MutableLiveData<String?>()

    fun onPublicDevice(): LiveData<Resource<UIDevice>> = onPublicDevice
    fun address(): LiveData<String?> = address

    fun fetchDevice(device: UIDevice?, currentHexSelected: UIHex?) {
        onPublicDevice.postValue(Resource.loading())
        viewModelScope.launch {
            if (device?.id == null || device.cellIndex == null) {
                Timber.w("Getting public device details failed: null")
                onPublicDevice.postValue(
                    Resource.error(resHelper.getString(R.string.error_public_device_no_data))
                )
                return@launch
            }
            if (device.address.isNullOrEmpty() && currentHexSelected != null) {
                launch {
                    fetchAddressFromHex(currentHexSelected)
                }
            } else if (!device.address.isNullOrEmpty()) {
                address.postValue(device.address)
            }

            val publicDevice = async {
                explorerUseCase.getPublicDevice(device.cellIndex, device.id)
            }

            val tokensDeferred = async {
                explorerUseCase.getTokenInfoLast30D(device.id)
            }

            var uiDevice: UIDevice? = null
            val publicDeviceResponse = publicDevice.await()
            publicDeviceResponse
                .map {
                    Timber.d("Got Public Device: $it")
                    uiDevice = it
                }
                .mapLeft {
                    analytics.trackEventFailure(it.code)
                    Timber.w("Getting public device details failed")
                    onPublicDevice.postValue(
                        Resource.error(resHelper.getString(R.string.error_public_device_no_data))
                    )
                    return@launch
                }

            val tokens = tokensDeferred.await()
            tokens
                .map {
                    uiDevice?.tokenInfo = it
                }
                .mapLeft {
                    analytics.trackEventFailure(it.code)
                    Timber.w("Getting public device token data failed")
                    onPublicDevice.postValue(
                        Resource.error(resHelper.getString(R.string.error_public_device_no_data))
                    )
                    return@launch
                }

            onPublicDevice.postValue(Resource.success(uiDevice))
        }
    }

    private suspend fun fetchAddressFromHex(currentHexSelected: UIHex) {
        address.postValue(explorerUseCase.getAddressOfHex(currentHexSelected))
    }
}
