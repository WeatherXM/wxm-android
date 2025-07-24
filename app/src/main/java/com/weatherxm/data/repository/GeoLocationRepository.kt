package com.weatherxm.data.repository

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.handleErrorWith
import arrow.core.left
import arrow.core.right
import com.mapbox.geojson.Point
import com.mapbox.search.result.ResultAccuracy
import com.mapbox.search.result.SearchAddress
import com.mapbox.search.result.SearchResult
import com.mapbox.search.result.SearchSuggestion
import com.weatherxm.data.datasource.CacheMapboxSearchDataSource
import com.weatherxm.data.datasource.NetworkMapboxSearchDataSource
import com.weatherxm.data.datasource.ReverseGeocodingDataSource
import com.weatherxm.data.models.CountryAndFrequencies
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.Location
import com.weatherxm.data.models.MapBoxError.ReverseGeocodingError
import timber.log.Timber

interface GeoLocationRepository {
    suspend fun getSearchSuggestions(query: String): Either<Failure, List<SearchSuggestion>>
    suspend fun getSuggestionLocation(suggestion: SearchSuggestion): Either<Failure, Location>
    suspend fun getAddressFromPoint(point: Point): Either<Failure, SearchAddress>
    suspend fun getCountryAndFrequencies(location: Location): CountryAndFrequencies
    suspend fun getUserCountryLocation(): Location?
}

class GeoLocationRepositoryImpl(
    private val reverseGeocodingDataSource: ReverseGeocodingDataSource,
    private val networkSearch: NetworkMapboxSearchDataSource,
    private val cacheSearch: CacheMapboxSearchDataSource
) : GeoLocationRepository {
    companion object {
        /**
         * This should stay at 100-200m because under 100m a lot of places are not being considered
         * whereas they should, like houses in the side of roads etc.
         */
        const val MAX_GEOCODING_DISTANCE_METERS = 200.0
    }

    override suspend fun getSearchSuggestions(
        query: String
    ): Either<Failure, List<SearchSuggestion>> {
        return cacheSearch.getSearchSuggestions(query, null)
            .onRight {
                Timber.d("Found suggestions in cache [query=$query]")
            }
            .handleErrorWith {
                val countryCode = reverseGeocodingDataSource.getUserCountry()
                networkSearch.getSearchSuggestions(query, countryCode).flatMap { countryResults ->
                    // If no results, search again globally without country limitations
                    if (countryResults.isEmpty()) {
                        networkSearch.getSearchSuggestions(query).onRight { globalResults ->
                            Timber.d("Saving suggestions in cache [query=$query]")
                            cacheSearch.setSearchSuggestions(query, globalResults)
                            return@flatMap Either.Right(globalResults)
                        }.onLeft {
                            return@flatMap Either.Left(it)
                        }
                    } else {
                        Timber.d("Saving suggestions in cache [query=$query]")
                        cacheSearch.setSearchSuggestions(query, countryResults)
                        Either.Right(countryResults)
                    }
                }
            }
    }

    override suspend fun getSuggestionLocation(
        suggestion: SearchSuggestion
    ): Either<Failure, Location> {
        return cacheSearch.getSuggestionLocation(suggestion)
            .onRight {
                Timber.d("Found suggestion location in cache [$suggestion]")
            }
            .handleErrorWith {
                networkSearch.getSuggestionLocation(suggestion)
                    .onRight {
                        Timber.d("Saving suggestion location in cache [$suggestion]")
                        cacheSearch.setSuggestionLocation(suggestion, it)
                    }
            }
    }

    override suspend fun getAddressFromPoint(point: Point): Either<Failure, SearchAddress> {
        return reverseGeocodingDataSource.getAddressFromPoint(point)
            .flatMap {
                if (!it.isAccurate()) {
                    Either.Left(ReverseGeocodingError.SearchResultNotAccurateError())
                } else if (!it.isNearby()) {
                    Either.Left(ReverseGeocodingError.SearchResultNotNearbyError())
                } else {
                    it.address?.right() ?: ReverseGeocodingError.SearchResultNoAddressError().left()
                }
            }
    }

    override suspend fun getCountryAndFrequencies(location: Location): CountryAndFrequencies {
        return reverseGeocodingDataSource.getCountryAndFrequencies(location)
            .fold({ CountryAndFrequencies.default() }, { it })
    }

    override suspend fun getUserCountryLocation(): Location? {
        return reverseGeocodingDataSource.getUserCountryLocation()
    }

    /**
     * Returns true if the search result's accuracy is one of the allowed values.
     */
    private fun SearchResult.isAccurate(): Boolean {
        return listOf(
            accuracy == ResultAccuracy.Point,
            accuracy == ResultAccuracy.Rooftop,
            accuracy == ResultAccuracy.Street
        ).any { it }
    }

    /**
     * Returns true if the search result's distance is within the max allowed value.
     */
    private fun SearchResult.isNearby(): Boolean {
        return distanceMeters?.let { it <= MAX_GEOCODING_DISTANCE_METERS } ?: false
    }
}
