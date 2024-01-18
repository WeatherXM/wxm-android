package com.weatherxm.ui.widgets.selectstation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.data.Resource
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.usecases.WidgetSelectStationUseCase
import com.weatherxm.util.Failure.getDefaultMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class SelectStationViewModel(private val usecase: WidgetSelectStationUseCase) : ViewModel() {

    private val devices = MutableLiveData<Resource<List<UIDevice>>>()
    private val isNotLoggedIn = MutableLiveData<Unit>()

    fun devices(): LiveData<Resource<List<UIDevice>>> = devices
    fun isNotLoggedIn(): LiveData<Unit> = isNotLoggedIn

    private var currentStationSelected = UIDevice.empty()

    fun setStationSelected(device: UIDevice) {
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
