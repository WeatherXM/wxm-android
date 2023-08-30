package com.weatherxm.ui.home.devices

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.data.ApiError
import com.weatherxm.data.Resource
import com.weatherxm.ui.common.DeviceRelation
import com.weatherxm.ui.common.UserDevices
import com.weatherxm.usecases.DeviceDetailsUseCase
import com.weatherxm.usecases.FollowUseCase
import com.weatherxm.util.Analytics
import com.weatherxm.util.ResourcesHelper
import com.weatherxm.util.UIErrors.getDefaultMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class DevicesViewModel : ViewModel(), KoinComponent {

    private val deviceDetailsUseCase: DeviceDetailsUseCase by inject()
    private val sharedPreferences: SharedPreferences by inject()
    private val followUseCase: FollowUseCase by inject()
    private val analytics: Analytics by inject()
    private val resHelper: ResourcesHelper by inject()

    private val sharedPreferenceChangeListener = OnSharedPreferenceChangeListener { _, _ ->
        this@DevicesViewModel.preferenceChanged.postValue(true)
    }

    private var userDevices: UserDevices? = null

    private val devices = MutableLiveData<Resource<UserDevices>>().apply {
        value = Resource.loading()
    }
    private val onFollowStatus = MutableLiveData<Resource<Unit>>()

    // Needed for passing info to the activity to show/hide elements when scrolling on the list
    private val showOverlayViews = MutableLiveData(true)

    // Needed for passing info to the fragment to notify the adapter that it needs updating
    private val preferenceChanged = MutableLiveData(false)

    fun devices(): LiveData<Resource<UserDevices>> = devices
    fun onFollowStatus(): LiveData<Resource<Unit>> = onFollowStatus
    fun showOverlayViews() = showOverlayViews
    fun preferenceChanged() = preferenceChanged
    fun getUserDevices() = userDevices

    fun fetch() {
        this@DevicesViewModel.devices.postValue(Resource.loading())
        viewModelScope.launch(Dispatchers.IO) {
            deviceDetailsUseCase.getUserDevices()
                .map { devices ->
                    Timber.d("Got ${devices.size} devices")
                    val ownedDevices = devices.count { it.relation == DeviceRelation.OWNED }
                    userDevices = UserDevices(
                        devices,
                        devices.size,
                        ownedDevices,
                        devices.size - ownedDevices
                    )
                    this@DevicesViewModel.devices.postValue(Resource.success(userDevices))
                }
                .mapLeft {
                    analytics.trackEventFailure(it.code)
                    this@DevicesViewModel.devices.postValue(Resource.error(it.getDefaultMessage()))
                }
        }
    }

    fun onScroll(dy: Int) {
        showOverlayViews.postValue(dy <= 0)
    }

    fun unFollowStation(deviceId: String) {
        onFollowStatus.postValue(Resource.loading())
        viewModelScope.launch {
            followUseCase.unfollowStation(deviceId).onRight {
                Timber.d("[Unfollow Device] Success")
                onFollowStatus.postValue(Resource.success(Unit))
                fetch()
            }.onLeft {
                Timber.e("[Unfollow Device] Error $it")
                analytics.trackEventFailure(it.code)
                val errorMessage = when (it) {
                    is ApiError.DeviceNotFound -> {
                        resHelper.getString(R.string.error_device_not_found)
                    }
                    is ApiError.MaxFollowed -> {
                        resHelper.getString(R.string.error_max_followed)
                    }
                    is ApiError.GenericError.JWTError.UnauthorizedError -> it.message
                    else -> it.getDefaultMessage(R.string.error_reach_out_short)
                } ?: resHelper.getString(R.string.error_reach_out_short)
                onFollowStatus.postValue(Resource.error(errorMessage))
            }
        }
    }

    init {
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)
    }
}
