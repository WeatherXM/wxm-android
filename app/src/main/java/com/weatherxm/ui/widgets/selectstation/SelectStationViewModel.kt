package com.weatherxm.ui.widgets.selectstation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.ui.common.Resource
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.usecases.WidgetSelectStationUseCase
import com.weatherxm.util.Failure.getDefaultMessage
import kotlinx.coroutines.launch
import timber.log.Timber

class SelectStationViewModel(private val usecase: WidgetSelectStationUseCase) : ViewModel() {

    private val onDevices = MutableLiveData<Resource<List<UIDevice>>>()
    private val isNotLoggedIn = MutableLiveData<Unit>()

    fun onDevices(): LiveData<Resource<List<UIDevice>>> = onDevices
    fun isNotLoggedIn(): LiveData<Unit> = isNotLoggedIn

    private var currentStationSelected = UIDevice.empty()

    fun setStationSelected(device: UIDevice) {
        currentStationSelected = device
    }

    fun getStationSelected() = currentStationSelected

    fun checkIfLoggedInAndProceed() {
        Timber.d("Checking if user is logged in in the background")
        if (usecase.isLoggedIn()) {
            fetch()
        } else {
            isNotLoggedIn.postValue(Unit)
        }
    }

    fun fetch() {
        onDevices.postValue(Resource.loading())
        viewModelScope.launch {
            usecase.getUserDevices().onRight { devices ->
                Timber.d("Got ${devices.size} devices")
                onDevices.postValue(Resource.success(devices))
            }.onLeft {
                onDevices.postValue(Resource.error(it.getDefaultMessage()))
            }
        }
    }

    fun saveWidgetData(widgetId: Int) {
        usecase.saveWidgetData(widgetId, currentStationSelected.id)
    }
}
