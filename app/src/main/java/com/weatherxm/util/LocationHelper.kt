package com.weatherxm.util

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.weatherxm.data.Location
import timber.log.Timber
import java.util.concurrent.TimeUnit

class LocationHelper(
    private val context: Context,
    private val locationClient: FusedLocationProviderClient
) {

    @Suppress("MagicNumber")
    @RequiresPermission(anyOf = [ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION])
    fun getLocationAndThen(onLocation: (location: Location?) -> Unit) {
        val priority = when (PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.checkSelfPermission(context, ACCESS_FINE_LOCATION) -> {
                Priority.PRIORITY_HIGH_ACCURACY
            }
            ActivityCompat.checkSelfPermission(context, ACCESS_COARSE_LOCATION) -> {
                Priority.PRIORITY_BALANCED_POWER_ACCURACY
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
                            LocationRequest.Builder(TimeUnit.SECONDS.toMillis(2))
                                .setMaxUpdates(1)
                                .setIntervalMillis(TimeUnit.SECONDS.toMillis(2))
                                .setMinUpdateIntervalMillis(0)
                                .setMaxUpdateDelayMillis(TimeUnit.SECONDS.toMillis(3))
                                .setPriority(it)
                                .build(),
                            object : LocationCallback() {
                                override fun onLocationResult(result: LocationResult) {
                                    result.lastLocation?.let {
                                        onLocation.invoke(Location(it.latitude, it.longitude))
                                    } ?: onLocation.invoke(null)
                                }
                            },
                            Looper.getMainLooper()
                        )
                    } else {
                        Timber.d("Got current location: $location")
                        onLocation.invoke(Location(location.latitude, location.longitude))
                    }
                }
                .addOnFailureListener {
                    Timber.d(it, "Could not get current location.")
                    onLocation.invoke(null)
                }
        }
    }

    fun hasLocationPermissions(): Boolean {
        val permGrantedCode = PackageManager.PERMISSION_GRANTED
        return ContextCompat.checkSelfPermission(context, ACCESS_FINE_LOCATION) == permGrantedCode
            || ContextCompat.checkSelfPermission(context, ACCESS_COARSE_LOCATION) == permGrantedCode
    }
}
