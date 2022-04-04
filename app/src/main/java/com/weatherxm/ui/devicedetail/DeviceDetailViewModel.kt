package com.weatherxm.ui.devicedetail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.weatherxm.R
import com.weatherxm.data.Device
import com.weatherxm.data.Resource
import com.weatherxm.util.ResourcesHelper
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class DeviceDetailViewModel : ViewModel(), KoinComponent {

    private val resHelper: ResourcesHelper by inject()
    private val device = MutableLiveData<Resource<Device>>(Resource.loading())

    fun device(): LiveData<Resource<Device>> = device

    fun setDevice(device: Device?) {
        this@DeviceDetailViewModel.device.postValue(
            if (device == null) {
                Timber.w("Getting public device details failed: null")
                Resource.error(resHelper.getString(R.string.error_public_device_no_data))
            } else {
                Timber.d("Got Public Device: $device")
                Resource.success(device)
            }
        )
    }
}
