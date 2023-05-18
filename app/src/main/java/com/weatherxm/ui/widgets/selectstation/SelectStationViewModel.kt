package com.weatherxm.ui.widgets.selectstation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.data.Device
import com.weatherxm.data.Resource
import com.weatherxm.usecases.WidgetSelectStationUseCase
import com.weatherxm.util.UIErrors.getDefaultMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class SelectStationViewModel : ViewModel(), KoinComponent {

    private val usecase: WidgetSelectStationUseCase by inject()

    private val devices = MutableLiveData<Resource<List<Device>>>()
    private val isNotLoggedIn = MutableLiveData<Unit>()

    fun devices(): LiveData<Resource<List<Device>>> = devices
    fun isNotLoggedIn(): LiveData<Unit> = isNotLoggedIn

    private var currentStationSelected = Device.empty()

    fun setStationSelected(device: Device) {
        currentStationSelected = device
    }

    fun getStationSelected() = currentStationSelected

    fun checkIfLoggedInAndProceed() {
        Timber.d("Checking if user is logged in in the background")
        viewModelScope.launch(Dispatchers.IO) {
            usecase.isLoggedIn().onRight {
                if (it) {
                    fetch()
                } else {
                    isNotLoggedIn.postValue(Unit)
                }
            }.onLeft {
                isNotLoggedIn.postValue(Unit)
            }
        }
    }

    fun fetch() {
        this@SelectStationViewModel.devices.postValue(Resource.loading())
        viewModelScope.launch(Dispatchers.IO) {
            usecase.getUserDevices()
                .map { devices ->
                    Timber.d("Got ${devices.size} devices")
                    this@SelectStationViewModel.devices.postValue(Resource.success(devices))
                }
                .mapLeft {
                    this@SelectStationViewModel.devices.postValue(
                        Resource.error(it.getDefaultMessage())
                    )
                }
        }
    }

    fun saveWidgetData(widgetId: Int) {
        usecase.saveWidgetData(widgetId, currentStationSelected.id)
    }
}
