package com.weatherxm.ui.claimdevice

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.weatherxm.R
import com.weatherxm.data.ApiError.DeviceNotFound
import com.weatherxm.data.ApiError.UserError.ClaimError.DeviceAlreadyClaimed
import com.weatherxm.data.ApiError.UserError.ClaimError.InvalidClaimId
import com.weatherxm.data.ApiError.UserError.ClaimError.InvalidClaimLocation
import com.weatherxm.data.Failure
import com.weatherxm.data.Resource
import com.weatherxm.usecases.ClaimDeviceUseCase
import com.weatherxm.util.ResourcesHelper
import com.weatherxm.util.UIErrors.getDefaultMessageResId
import com.weatherxm.util.Validator
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.util.concurrent.TimeUnit

/*
* This suppress is needed because of the complexity of the claiming process where a lot of
* fragments and an activity are involved and communication is needed between them
*/
@Suppress("TooManyFunctions")
class ClaimDeviceViewModel : ViewModel(), KoinComponent {
    // Current arbitrary values for the viewpager and the map
    companion object {
        const val ZOOM_LEVEL: Double = 15.0
    }

    private val claimDeviceUseCase: ClaimDeviceUseCase by inject()
    private val resHelper: ResourcesHelper by inject()
    private val validator: Validator by inject()

    private lateinit var locationClient: FusedLocationProviderClient

    private lateinit var currentSerialNumber: String
    private var isSerialSet = false
    private var userEmail: String? = null
    private var isLocationSet = false
    private var currentLon: Double = 0.0
    private var currentLat: Double = 0.0

    private val onDeviceLocation = MutableLiveData<Location>()
    private val onLocationSet = MutableLiveData(false)
    private val onNextButtonEnabledStatus = MutableLiveData(true)
    private val onNextButtonClick = MutableLiveData(false)
    private val onCheckSerialAndContinue = MutableLiveData(false)
    private val onClaimResult = MutableLiveData<Resource<String>>().apply {
        value = Resource.loading()
    }

    fun onNextButtonEnabledStatus() = onNextButtonEnabledStatus
    fun onNextButtonClick() = onNextButtonClick
    fun onCheckSerialAndContinue() = onCheckSerialAndContinue
    fun onDeviceLocation() = onDeviceLocation
    fun onLocationSet() = onLocationSet
    fun onClaimResult() = onClaimResult

    private fun setSerialNumber(serialNumber: String) {
        currentSerialNumber = serialNumber
        setSerialSet(true)
        nextButtonClick()
    }

    fun setSerialSet(isSet: Boolean) {
        isSerialSet = isSet
    }

    fun getSerialNumber(): String {
        return currentSerialNumber
    }

    fun getUserEmail(): String? {
        return userEmail
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
            setLocationSet(true)
        }
    }

    fun setLocationSet(locationSet: Boolean) {
        isLocationSet = locationSet
    }

    fun getLocationSet(): Boolean {
        return isLocationSet
    }

    fun claimDevice() {
        if (isLocationSet) {
            onClaimResult.postValue(Resource.loading())
            viewModelScope.launch {
                claimDeviceUseCase.claimDevice(currentSerialNumber, currentLat, currentLon)
                    .map {
                        Timber.d("Claimed device: $it")
                        onClaimResult.postValue(Resource.success(it.name))
                    }
                    .mapLeft {
                        handleFailure(it)
                    }
            }
        }
    }

    private fun handleFailure(failure: Failure) {
        onClaimResult.postValue(
            Resource.error(
                resHelper.getString(
                    when (failure) {
                        is InvalidClaimId -> R.string.error_claim_invalid_serial
                        is InvalidClaimLocation -> R.string.error_claim_invalid_location
                        is DeviceAlreadyClaimed -> R.string.error_claim_device_already_claimed
                        is DeviceNotFound -> R.string.error_claim_not_found
                        else -> failure.getDefaultMessageResId()
                    }
                )
            )
        )
    }

    fun fetchUserEmail() {
        viewModelScope.launch {
            claimDeviceUseCase.fetchUserEmail()
                .map {
                    userEmail = it
                }
                .mapLeft {
                    userEmail = null
                }
        }
    }

    fun nextButtonStatus(enabled: Boolean) {
        onNextButtonEnabledStatus.postValue(enabled)
    }

    fun nextButtonClick() {
        onNextButtonClick.postValue(true)
    }

    fun isSerialSet(): Boolean {
        return isSerialSet
    }

    fun checkSerialAndContinue() {
        onCheckSerialAndContinue.postValue(true)
    }

    fun validateAndSetSerial(serialNumber: String): Boolean {
        return validator.validateSerialNumber(serialNumber).apply {
            if (this) {
                setSerialNumber(serialNumber)
            } else {
                setSerialSet(false)
            }
        }
    }

    @Suppress("MagicNumber")
    @RequiresPermission(anyOf = [ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION])
    fun getLocationAndThen(context: Context, onLocation: (location: Location?) -> Unit) {
        locationClient = LocationServices.getFusedLocationProviderClient(context)
        val priority = when (PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.checkSelfPermission(context, ACCESS_FINE_LOCATION) -> {
                LocationRequest.PRIORITY_HIGH_ACCURACY
            }
            ActivityCompat.checkSelfPermission(context, ACCESS_COARSE_LOCATION) -> {
                LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
            }
            else -> {
                null
            }
        }
        priority?.let {
            locationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location == null) {
                        Timber.d("Current location is null. Requesting fresh location.")
                        locationClient.requestLocationUpdates(
                            LocationRequest.create()
                                .setNumUpdates(1)
                                .setInterval(TimeUnit.SECONDS.toMillis(2))
                                .setFastestInterval(0)
                                .setMaxWaitTime(TimeUnit.SECONDS.toMillis(3))
                                .setPriority(it),
                            object : LocationCallback() {
                                override fun onLocationResult(result: LocationResult) {
                                    onLocation.invoke(result.lastLocation)
                                }
                            },
                            Looper.getMainLooper()
                        )
                    } else {
                        Timber.d("Got current location: $location")
                        onLocation.invoke(location)
                    }
                }
                .addOnFailureListener {
                    Timber.d(it, "Could not get current location.")
                    onLocation.invoke(null)
                }
        }
    }
}
