package com.weatherxm.ui.publicdeviceslist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.data.Resource
import com.weatherxm.ui.UIDevice
import com.weatherxm.ui.UIHex
import com.weatherxm.usecases.ExplorerUseCase
import com.weatherxm.util.ResourcesHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class PublicDevicesListViewModel : ViewModel(), KoinComponent {
    private val resHelper: ResourcesHelper by inject()
    private val explorerUseCase: ExplorerUseCase by inject()
    private val onPublicDevices = MutableLiveData<Resource<List<UIDevice>>>(Resource.loading())
    private val address = MutableLiveData<String>()

    fun onPublicDevices(): LiveData<Resource<List<UIDevice>>> = onPublicDevices
    fun address(): LiveData<String> = address

    fun fetchDevices(uiHex: UIHex?) {
        onPublicDevices.postValue(Resource.loading())
        viewModelScope.launch(Dispatchers.IO) {
            if (uiHex == null || uiHex.index.isEmpty()) {
                Timber.w("Getting public devices failed: hexIndex = null")
                onPublicDevices.postValue(
                    Resource.error(resHelper.getString(R.string.error_public_devices_no_data))
                )
                return@launch
            }

            explorerUseCase.getPublicDevicesOfHex(uiHex)
                .map {
                    onPublicDevices.postValue(Resource.success(it))
                    it.forEach { device ->
                        device.address?.let {
                            this@PublicDevicesListViewModel.address.postValue(it)
                            return@forEach
                        }
                    }

                }
                .mapLeft {
                    onPublicDevices.postValue(
                        Resource.error(resHelper.getString(R.string.error_public_devices_no_data))
                    )
                }
        }
    }
}
