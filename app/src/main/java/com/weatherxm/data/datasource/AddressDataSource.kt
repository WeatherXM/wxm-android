package com.weatherxm.data.datasource

import android.content.Context
import android.content.SharedPreferences
import android.location.Geocoder
import arrow.core.Either
import com.mapbox.geojson.Point
import com.mapbox.search.QueryType
import com.mapbox.search.ResponseInfo
import com.mapbox.search.ReverseGeoOptions
import com.mapbox.search.SearchCallback
import com.mapbox.search.SearchEngine
import com.mapbox.search.common.SearchCancellationException
import com.mapbox.search.result.SearchResult
import com.weatherxm.data.CancellationError
import com.weatherxm.data.DataError
import com.weatherxm.data.Failure
import com.weatherxm.data.Location
import com.weatherxm.data.MapBoxError
import timber.log.Timber
import java.io.IOException
import java.util.*
import kotlin.coroutines.suspendCoroutine

interface AddressDataSource {
    suspend fun getLocationAddress(
        hexIndex: String,
        location: Location,
        locale: Locale = Locale.getDefault()
    ): Either<Failure, String?>

    suspend fun setLocationAddress(hexIndex: String, address: String): Either<Failure, Unit>

    suspend fun getAddressFromPoint(point: Point): Either<Failure, SearchResult>
}

class NetworkAddressDataSource(
    private val context: Context,
    private val mapBoxSearchEngine: SearchEngine
) : AddressDataSource {
    companion object {
        /**
         * Limit of search results
         */
        private const val SEARCH_LIMIT = 1
        private val SEARCH_TYPES = mutableListOf(
            QueryType.ADDRESS,
            QueryType.NEIGHBORHOOD,
            QueryType.DISTRICT,
            QueryType.LOCALITY,
            QueryType.PLACE,
            QueryType.POSTCODE
        )
    }

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
        throw NotImplementedError()
    }

    override suspend fun getAddressFromPoint(point: Point): Either<Failure, SearchResult> {
        return suspendCoroutine { continuation ->
            mapBoxSearchEngine.search(
                ReverseGeoOptions(center = point, limit = SEARCH_LIMIT, types = SEARCH_TYPES),
                object : SearchCallback {
                    override fun onError(e: Exception) {
                        if (e is SearchCancellationException) {
                            Timber.d("Search was cancelled due to new point [point=$point]")
                            continuation.resumeWith(Result.success(Either.Left(CancellationError)))
                        } else {
                            Timber.w(e, "Reverse Geocoding failure [point=$point]")
                            continuation.resumeWith(
                                Result.success(Either.Left(MapBoxError.GeocodingError))
                            )
                        }
                    }

                    override fun onResults(
                        results: List<SearchResult>,
                        responseInfo: ResponseInfo
                    ) {
                        continuation.resumeWith(
                            Result.success(
                                results.firstOrNull()?.let {
                                    Either.Right(it)
                                } ?: Either.Left(MapBoxError.GeocodingError)
                            )
                        )
                    }
                }
            )
        }
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

    override suspend fun getAddressFromPoint(point: Point): Either<Failure, SearchResult> {
        throw NotImplementedError("Won't be implemented. Ignore this.")
    }
}
