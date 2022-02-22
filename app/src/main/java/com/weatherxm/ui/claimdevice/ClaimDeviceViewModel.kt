package com.weatherxm.ui.claimdevice

import android.location.Location
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.weatherxm.R
import com.weatherxm.data.Failure
import com.weatherxm.data.Resource
import com.weatherxm.data.ServerError
import com.weatherxm.usecases.ClaimDeviceUseCase
import com.weatherxm.util.ResourcesHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

// TODO: Find an implementation with less functions??
@Suppress("TooManyFunctions")
class ClaimDeviceViewModel : ViewModel(), KoinComponent {
    // Current arbitrary values for the viewpager and the map
    companion object {
        const val ZOOM_LEVEL: Double = 15.0
    }

    private val claimDeviceUseCase: ClaimDeviceUseCase by inject()
    private val resHelper: ResourcesHelper by inject()

    private lateinit var currentSerialNumber: String
    private var userEmail: String? = null
    private var isLocationSet = false
    private var currentLon: Double = 0.0
    private var currentLat: Double = 0.0

    // Needed for passing info to the activity to move to the next page in the view pager or use gps
    private val onStep = MutableLiveData(0)
    private val onGPS = MutableLiveData(false)
    private val onDeviceLocation = MutableLiveData<Location>()
    private val onLocationSet = MutableLiveData(false)
    private val onClaimResult = MutableLiveData<Resource<String>>().apply {
        value = Resource.loading()
    }

    fun onStep() = onStep
    fun onGPS() = onGPS
    fun onDeviceLocation() = onDeviceLocation
    fun onLocationSet() = onLocationSet
    fun onClaimResult() = onClaimResult

    fun next() = onStep.postValue(1)

    fun previous() = onStep.postValue(-1)

    fun setSerialNumber(serialNumber: String) {
        currentSerialNumber = serialNumber
    }

    fun getSerialNumber(): String {
        return currentSerialNumber
    }

    fun getUserEmail(): String? {
        return userEmail
    }

    fun useGps() {
        onGPS.postValue(true)
    }

    fun updateLocationOnMap(location: Location) {
        onDeviceLocation.postValue(location)
    }

    fun setLocationInvoke() {
        onLocationSet.postValue(true)
    }

    fun setLocation(lon: Double?, lat: Double?) {
        if (lon != null && lat != null) {
            currentLon = lon
            currentLat = lat
            isLocationSet = true
        }
    }

    fun claimDevice() {
        if (isLocationSet) {
            onClaimResult.postValue(Resource.loading())
            CoroutineScope(Dispatchers.IO).launch {
                claimDeviceUseCase.claimDevice(currentSerialNumber, currentLat, currentLon)
                    .map {
                        Timber.d("Claimed device: $it")
                        onClaimResult.postValue(
                            Resource.success(
                                resHelper.getString(
                                    R.string.success_claim_device,
                                    it.name
                                )
                            )
                        )
                    }
                    .mapLeft {
                        Timber.w("Claiming device failed: $it")
                        when (it) {
                            is Failure.NetworkError -> onClaimResult.postValue(
                                Resource.error(resHelper.getString(R.string.network_error))
                            )
                            is ServerError -> onClaimResult.postValue(
                                Resource.error(resHelper.getString(R.string.server_error))
                            )
                            is Failure.UnknownError -> onClaimResult.postValue(
                                Resource.error(resHelper.getString(R.string.unknown_error))
                            )
                        }
                    }
            }
        }
    }

    fun fetchUserEmail() {
        CoroutineScope(Dispatchers.IO).launch {
            claimDeviceUseCase.fetchUserEmail()
                .map {
                    userEmail = it
                }
                .mapLeft {
                    userEmail = null
                }
        }
    }
}
