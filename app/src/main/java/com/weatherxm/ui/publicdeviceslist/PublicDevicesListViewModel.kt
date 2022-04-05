package com.weatherxm.ui.publicdeviceslist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.weatherxm.R
import com.weatherxm.data.Device
import com.weatherxm.data.Resource
import com.weatherxm.usecases.ExplorerUseCase
import com.weatherxm.util.ResourcesHelper
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class PublicDevicesListViewModel : ViewModel(), KoinComponent {
    private val resHelper: ResourcesHelper by inject()
    private val explorerUseCase: ExplorerUseCase by inject()
    private val devices = MutableLiveData<Resource<List<Device>>>(Resource.loading())
    private val address = MutableLiveData<String>()

    fun devices(): LiveData<Resource<List<Device>>> = devices
    fun address(): LiveData<String> = address

    fun fetchDevices(hexIndex: String?) {
        val devicesOfH7 = explorerUseCase.getDevicesOfH7(hexIndex)
        this@PublicDevicesListViewModel.devices.postValue(
            if (devicesOfH7.isNullOrEmpty()) {
                Timber.w("Getting public devices failed: null")
                Resource.error(resHelper.getString(R.string.error_public_devices_no_data))
            } else {
                Timber.d("Got Public Devices: $devices")
                Resource.success(devicesOfH7.sortedByDescending { it.attributes?.isActive })
            }
        )
        devicesOfH7?.forEach { device ->
            device.address?.let {
                this@PublicDevicesListViewModel.address.postValue(it)
                return@forEach
            }
        }
    }
}
