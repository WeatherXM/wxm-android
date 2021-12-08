package com.weatherxm.ui.home.devices

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.weatherxm.R
import com.weatherxm.data.Device
import com.weatherxm.data.Failure
import com.weatherxm.data.Resource
import com.weatherxm.data.ServerError
import com.weatherxm.data.repository.DeviceRepository
import com.weatherxm.util.ResourcesHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class DevicesViewModel : ViewModel(), KoinComponent {

    private val deviceRepository: DeviceRepository by inject()
    private val resHelper: ResourcesHelper by inject()

    private val devices = MutableLiveData<Resource<List<Device>>>().apply {
        value = Resource.loading()
    }

    fun devices(): LiveData<Resource<List<Device>>> = devices
    fun resHelper(): ResourcesHelper = resHelper

    fun fetch() {
        CoroutineScope(Dispatchers.IO).launch {
            deviceRepository.getUserDevices()
                .map { devices ->
                    Timber.d("Got devices: $devices")
                    this@DevicesViewModel.devices.postValue(Resource.success(devices))
                }
                .mapLeft {
                    Timber.d("Got error: $it")
                    when (it) {
                        is Failure.NetworkError -> devices.postValue(
                            Resource.error(resHelper.getString(R.string.network_error))
                        )
                        is ServerError -> devices.postValue(
                            Resource.error(resHelper.getString(R.string.server_error))
                        )
                        is Failure.UnknownError -> devices.postValue(
                            Resource.error(resHelper.getString(R.string.unknown_error))
                        )
                        else -> devices.postValue(
                            Resource.error(resHelper.getString(R.string.unknown_error))
                        )
                    }
                }
        }
    }
}
