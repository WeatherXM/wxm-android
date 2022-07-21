package com.weatherxm.data.datasource

import android.content.Context
import android.content.SharedPreferences
import android.location.Geocoder
import arrow.core.Either
import com.weatherxm.data.DataError
import com.weatherxm.data.Failure
import com.weatherxm.data.Location
import timber.log.Timber
import java.io.IOException
import java.util.*

interface AddressDataSource {
    suspend fun getLocationAddress(
        hexIndex: String,
        location: Location,
        locale: Locale = Locale.getDefault()
    ): Either<Failure, String?>

    suspend fun setLocationAddress(hexIndex: String, address: String): Either<Failure, Unit>
}

class NetworkAddressDataSource(private val context: Context) : AddressDataSource {
    override suspend fun getLocationAddress(
        hexIndex: String,
        location: Location,
        locale: Locale
    ): Either<Failure, String?> {
        /*
        * Google Says: https://developer.android.com/reference/android/location/Geocoder
        * This method was deprecated in API level Tiramisu.
        * Use getFromLocation(double, double, int, android.location.Geocoder.GeocodeListener)
        * instead to avoid blocking a thread waiting for results.
        * ---
        * But, is that the case here? We already run this function outside of the UI thread,
        * and we need to wait for the results actually.
        */
        return if (Geocoder.isPresent()) {
            try {
                val geocoderAddresses =
                    Geocoder(context, locale).getFromLocation(location.lat, location.lon, 1)

                if (geocoderAddresses.isNullOrEmpty()) {
                    Either.Left(Failure.LocationAddressNotFound)
                } else {
                    val geocoderAddress = geocoderAddresses[0]
                    Either.Right(
                        if (geocoderAddress.locality != null) {
                            "${geocoderAddress.locality}, ${geocoderAddress.countryCode}"
                        } else if (geocoderAddress.subAdminArea != null) {
                            "${geocoderAddress.subAdminArea}, ${geocoderAddress.countryCode}"
                        } else if (geocoderAddress.adminArea != null) {
                            "${geocoderAddress.adminArea}, ${geocoderAddress.countryCode}"
                        } else {
                            geocoderAddress.countryName
                        }
                    )
                }
            } catch (exception: IOException) {
                Timber.w(exception, "Geocoder failed with: IOException.")
                Either.Left(Failure.UnknownError)
            }
        } else {
            Either.Left(Failure.NoGeocoderError)
        }
    }

    override suspend fun setLocationAddress(
        hexIndex: String,
        address: String
    ): Either<Failure, Unit> {
        TODO("Won't be implemented. Ignore this.")
    }
}

/**
 * Simple shared preference database with key and value both strings.
 */
class StorageAddressDataSource(private val preferences: SharedPreferences) : AddressDataSource {

    override suspend fun getLocationAddress(
        hexIndex: String,
        location: Location,
        locale: Locale
    ): Either<Failure, String?> {
        return preferences.getString(hexIndex, null)?.let {
            Either.Right(it)
        } ?: Either.Left(DataError.DatabaseMissError)
    }

    override suspend fun setLocationAddress(
        hexIndex: String,
        address: String
    ): Either<Failure, Unit> {
        preferences.edit().putString(hexIndex, address).apply()
        return Either.Right(Unit)
    }
}
