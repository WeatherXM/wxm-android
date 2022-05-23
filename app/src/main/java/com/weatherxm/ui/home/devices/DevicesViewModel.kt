package com.weatherxm.ui.home.devices

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.data.Device
import com.weatherxm.data.Failure
import com.weatherxm.data.Resource
import com.weatherxm.usecases.UserDeviceUseCase
import com.weatherxm.util.UIErrors.getDefaultMessage
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class DevicesViewModel : ViewModel(), KoinComponent {

    private val userDeviceUseCase: UserDeviceUseCase by inject()

    private val devices = MutableLiveData<Resource<List<Device>>>().apply {
        value = Resource.loading()
    }

    // Needed for passing info to the activity to show/hide elements when scrolling on the list
    private val showOverlayViews = MutableLiveData(true)

    fun devices(): LiveData<Resource<List<Device>>> = devices

    fun showOverlayViews() = showOverlayViews

    fun fetch() {
        this@DevicesViewModel.devices.postValue(Resource.loading())
        viewModelScope.launch {
            userDeviceUseCase.getUserDevices()
                .map { devices ->
                    Timber.d("Got devices: $devices")
                    this@DevicesViewModel.devices.postValue(Resource.success(devices))
                }
                .mapLeft {
                    handleFailure(it)
                }
        }
    }

    fun onScroll(dy: Int) {
        showOverlayViews.postValue(dy <= 0)
    }

    private fun handleFailure(failure: Failure) {
        devices.postValue(Resource.error(failure.getDefaultMessage()))
    }
}
