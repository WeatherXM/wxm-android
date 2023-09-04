package com.weatherxm.util

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.annotation.RequiresApi
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.weatherxm.data.Failure.GeocoderError
import com.weatherxm.data.Location
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.io.IOException
import java.util.Locale

object GeocoderCompat {
    /** Max results to return */
    private const val MAX_RESULTS = 1

    /**
     * Compatibility method that uses the new Geocoder in API 33+
     * and defaults to the old Geocoder in older API versions.
     * Returns a non-null Address, or GeocoderError.
     */
    suspend fun getFromLocation(
        context: Context,
        location: Location,
        locale: Locale = Locale.getDefault()
    ): Either<GeocoderError, Address> {
        return getFromLocation(context, location.lat, location.lon, locale)
    }

    /**
     * Compatibility method that uses the new Geocoder in API 33+
     * and defaults to the old Geocoder in older API versions.
     * Returns a non-null Address, or GeocoderError.
     */
    suspend fun getFromLocation(
        context: Context,
        latitude: Double,
        longitude: Double,
        locale: Locale = Locale.getDefault()
    ): Either<GeocoderError, Address> {
        return try {
            if (!Geocoder.isPresent()) {
                Timber.d("Geocoder not present on device.")
                return GeocoderError.NoGeocoderError.left()
            }

            // TODO Future optimization, don't create a new Geocoder instance every time.
            val geocoder = Geocoder(context, locale)

            val address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocationApi33(latitude, longitude)
            } else {
                geocoder.getFromLocationApi1(latitude, longitude)
            }

            Timber.d("Geocoded ($latitude, $longitude): $address")

            address?.right() ?: GeocoderError.NoGeocodedAddressError.left()
        } catch (exception: IOException) {
            Timber.w(exception, "Geocoder failed with IOException.")
            GeocoderError.GeocoderIOError.left()
        }
    }

    @Suppress("DEPRECATION")
    private fun Geocoder.getFromLocationApi1(
        latitude: Double,
        longitude: Double
    ): Address? {
        return this.getFromLocation(latitude, longitude, MAX_RESULTS)?.firstOrNull()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun Geocoder.getFromLocationApi33(
        latitude: Double,
        longitude: Double
    ) = suspendCancellableCoroutine { continuation ->
        this.getFromLocation(latitude, longitude, MAX_RESULTS) { addresses ->
            continuation.resumeWith(Result.success(addresses.firstOrNull()))
        }
    }
}
