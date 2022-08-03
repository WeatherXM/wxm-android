package com.weatherxm.ui.home.devices

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.data.Device
import com.weatherxm.data.Failure
import com.weatherxm.data.Resource
import com.weatherxm.usecases.UserDeviceUseCase
import com.weatherxm.util.UIErrors.getDefaultMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class DevicesViewModel : ViewModel(), KoinComponent {

    private val userDeviceUseCase: UserDeviceUseCase by inject()
    private val sharedPreferences: SharedPreferences by inject()

    private val sharedPreferenceChangeListener = OnSharedPreferenceChangeListener { _, _ ->
        this@DevicesViewModel.preferenceChanged.postValue(true)
    }

    private val devices = MutableLiveData<Resource<List<Device>>>().apply {
        value = Resource.loading()
    }

    // Needed for passing info to the activity to show/hide elements when scrolling on the list
    private val showOverlayViews = MutableLiveData(true)

    // Needed for passing info to the fragment to notify the adapter that it needs updating
    private val preferenceChanged = MutableLiveData(false)

    fun devices(): LiveData<Resource<List<Device>>> = devices

    fun showOverlayViews() = showOverlayViews

    fun preferenceChanged() = preferenceChanged

    fun fetch() {
        this@DevicesViewModel.devices.postValue(Resource.loading())
        viewModelScope.launch(Dispatchers.IO) {
            userDeviceUseCase.getUserDevices()
                .map { devices ->
                    Timber.d("Got ${devices.size} devices")
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

    init {
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)
    }
}
