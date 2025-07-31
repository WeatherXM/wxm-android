package com.weatherxm.data.datasource

import android.content.Context
import android.content.Context.TELEPHONY_SERVICE
import android.telephony.TelephonyManager
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
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.weatherxm.data.countryToFrequency
import com.weatherxm.data.models.CancellationError
import com.weatherxm.data.models.CountryAndFrequencies
import com.weatherxm.data.models.CountryInfo
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.Location
import com.weatherxm.data.models.MapBoxError
import com.weatherxm.data.otherFrequencies
import com.weatherxm.data.services.CacheService
import com.weatherxm.util.GeocoderCompat
import org.json.JSONException
import timber.log.Timber
import kotlin.coroutines.suspendCoroutine

interface ReverseGeocodingDataSource {
    suspend fun getAddressFromPoint(point: Point): Either<Failure, SearchResult>
    suspend fun getCountryAndFrequencies(location: Location): Either<Failure, CountryAndFrequencies>
    fun getUserCountry(): String?
    suspend fun getUserCountryLocation(): Location?
}

class ReverseGeocodingDataSourceImpl(
    private val context: Context,
    private val mapBoxSearchEngine: SearchEngine,
    private val moshi: Moshi,
    private val cacheService: CacheService
) : ReverseGeocodingDataSource {
    companion object {
        /**
         * Limit of search results
         */
        const val SEARCH_LIMIT = 1
        val SEARCH_TYPES = mutableListOf(
            QueryType.ADDRESS,
            QueryType.NEIGHBORHOOD,
            QueryType.DISTRICT,
            QueryType.LOCALITY,
            QueryType.PLACE,
            QueryType.POSTCODE
        )
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
                } ?: Failure.CountryNotFound.left()
            }
            .mapLeft {
                Failure.CountryNotFound
            }
    }

    // https://stackoverflow.com/questions/3659809/where-am-i-get-country
    override fun getUserCountry(): String? {
        val telephonyManager = context.getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        val simCountry = telephonyManager.simCountryIso
        val networkCountry = telephonyManager.networkCountryIso

        return if (simCountry.length == 2) {
            Timber.d("Found user's country via SIM: [country=$simCountry]")
            simCountry.uppercase()
        } else if (networkCountry.length == 2) {
            Timber.d("Found user's country via Network: [country=$networkCountry]")
            networkCountry.uppercase()
        } else {
            null
        }
    }

    override suspend fun getUserCountryLocation(): Location? {
        return getUserCountry()?.let { code ->
            cacheService.getCountriesInfo().ifEmpty {
                parseCountriesJson()
            }?.firstOrNull {
                it.code == code && it.mapCenter != null
            }?.mapCenter
        }
    }

    private fun parseCountriesJson(): List<CountryInfo>? {
        return try {
            val adapter: JsonAdapter<List<CountryInfo>> = moshi.adapter(
                Types.newParameterizedType(List::class.java, CountryInfo::class.java)
            )
            adapter.fromJson(
                context.assets.open("countries_information.json")
                    .bufferedReader()
                    .use {
                        it.readText()
                    }).apply {
                cacheService.setCountriesInfo(this)
            }
        } catch (e: JSONException) {
            Timber.w(e, "Failure: JSON Parsing of countries information")
            null
        }
    }
}
