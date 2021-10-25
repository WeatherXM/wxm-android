package com.weatherxm.ui.devicedetail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import arrow.core.Either
import com.weatherxm.R
import com.weatherxm.data.PublicDevice
import com.weatherxm.data.Resource
import com.weatherxm.data.repository.DeviceRepository
import com.weatherxm.util.ResourcesHelper
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class DeviceDetailViewModel : ViewModel(), KoinComponent {

    private val onDeviceDetailsUpdate = MutableLiveData<Resource<PublicDevice>>().apply {
        value = Resource.loading()
    }

    fun onDeviceDetailsUpdate(): LiveData<Resource<PublicDevice>> = onDeviceDetailsUpdate

    private val deviceRepository: DeviceRepository by inject()
    private val resourcesHelper: ResourcesHelper by inject()

    fun fetch(device: PublicDevice?) {
        if (device == null) {
            this@DeviceDetailViewModel.onDeviceDetailsUpdate.postValue(
                Resource.error(resourcesHelper.getString(R.string.no_data_error_device_details))
            )
        } else {
            Timber.d("Got Public Device: $device")
            this@DeviceDetailViewModel.onDeviceDetailsUpdate.postValue(Resource.success(device))
        }
    }

    fun getNameOrLabel(name: String, label: String?): Resource<String> {
        return when (val nameOrLabelOfDevice = deviceRepository.getNameOrLabel(name, label)) {
            is Either.Left -> {
                Resource.error(resourcesHelper.getString(R.string.name_not_found_error))
            }
            is Either.Right -> {
                Resource.success(nameOrLabelOfDevice.value)
            }
        }
    }
}
