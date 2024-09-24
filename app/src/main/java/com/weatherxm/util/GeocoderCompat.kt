package com.weatherxm.util

import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.annotation.RequiresApi
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.weatherxm.data.models.Failure.GeocoderError
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.io.IOException

object GeocoderCompat : KoinComponent {
    private val geocoder: Geocoder by inject()

    /** Max results to return */
    private const val MAX_RESULTS = 1

    /**
     * Compatibility method that uses the new Geocoder in API 33+
     * and defaults to the old Geocoder in older API versions.
     * Returns a non-null Address, or GeocoderError.
     */
    suspend fun getFromLocation(
        latitude: Double,
        longitude: Double
    ): Either<GeocoderError, Address> {
        return try {
            if (!Geocoder.isPresent()) {
                Timber.d("Geocoder not present on device.")
                return GeocoderError.NoGeocoderError.left()
            }

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
