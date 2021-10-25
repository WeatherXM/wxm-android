package com.weatherxm.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import arrow.core.flatMap
import com.weatherxm.data.Device
import com.weatherxm.data.Resource
import com.weatherxm.data.repository.AuthRepository
import com.weatherxm.data.repository.DeviceRepository
import com.weatherxm.data.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class HomeViewModel : ViewModel(), KoinComponent {

    private val userRepository: UserRepository by inject()
    private val deviceRepository: DeviceRepository by inject()
    private val repository: AuthRepository by inject()

    private val mutableSelectedDevice = MutableLiveData<Device>()
    val selectedDevice: LiveData<Device> get() = mutableSelectedDevice

    private val devices = MutableLiveData<Resource<List<Device>>>().apply {
        value = Resource.loading()
    }
    fun devices(): LiveData<Resource<List<Device>>> = devices

    fun fetch() {
        CoroutineScope(Dispatchers.IO).launch {
            userRepository.getUser()
                .flatMap { user -> deviceRepository.getCustomerDevices(user.customerId.id) }
                .map { devices ->
                    Timber.d("Got devices: $devices")
                    this@HomeViewModel.devices.postValue(Resource.success(devices))
                }
        }
    }

    fun selectDevice(device: Device) {
        mutableSelectedDevice.value = device
    }

    fun logout() {
        CoroutineScope(Dispatchers.Default).launch {
            repository.logout()
        }
    }
}
