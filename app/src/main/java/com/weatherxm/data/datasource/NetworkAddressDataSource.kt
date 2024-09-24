package com.weatherxm.data.datasource

import android.content.Context
import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import com.mapbox.geojson.Point
import com.mapbox.search.QueryType
import com.mapbox.search.ResponseInfo
import com.mapbox.search.ReverseGeoOptions
import com.mapbox.search.SearchCallback
import com.mapbox.search.SearchEngine
import com.mapbox.search.common.SearchCancellationException
import com.mapbox.search.result.SearchResult
import com.squareup.moshi.Moshi
import com.weatherxm.data.models.CancellationError
import com.weatherxm.data.models.CountryAndFrequencies
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.Location
import com.weatherxm.data.models.MapBoxError
import com.weatherxm.data.countryToFrequency
import com.weatherxm.data.otherFrequencies
import com.weatherxm.util.GeocoderCompat
import timber.log.Timber
import kotlin.coroutines.suspendCoroutine

class NetworkAddressDataSource(
    private val context: Context,
    private val mapBoxSearchEngine: SearchEngine,
    private val moshi: Moshi
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
        location: Location
    ): Either<Failure, String> {
        return GeocoderCompat.getFromLocation(location.lat, location.lon)
            .map { address ->
                if (address.locality != null) {
                    "${address.locality}, ${address.countryCode}"
                } else if (address.subAdminArea != null) {
                    "${address.subAdminArea}, ${address.countryCode}"
                } else if (address.adminArea != null) {
                    "${address.adminArea}, ${address.countryCode}"
                } else {
                    address.countryName
                }
            }
    }

    override suspend fun setLocationAddress(
        hexIndex: String,
        address: String
    ): Either<Failure, Unit> {
        throw NotImplementedError("Won't be implemented. Ignore this.")
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
                                Result.success(Either.Left(MapBoxError.GeocodingError()))
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
                                } ?: Either.Left(MapBoxError.GeocodingError())
                            )
                        )
                    }
                }
            )
        }
    }

    override suspend fun getCountryAndFrequencies(
        location: Location
    ): Either<Failure, CountryAndFrequencies> {
        return GeocoderCompat.getFromLocation(location.lat, location.lon)
            .flatMap { address ->
                countryToFrequency(context, address.countryCode, moshi)?.let { frequency ->
                    CountryAndFrequencies(
                        address.countryName, frequency, otherFrequencies(frequency)
                    ).right()
                } ?: Failure.FrequencyMappingNotFound.left()
            }
            .mapLeft {
                Failure.CountryNotFound
            }
    }
}
