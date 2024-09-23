package com.weatherxm.data.repository

import arrow.core.Either
import arrow.core.handleErrorWith
import com.mapbox.search.result.SearchSuggestion
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.Location
import com.weatherxm.data.datasource.CacheAddressSearchDataSource
import com.weatherxm.data.datasource.LocationDataSource
import com.weatherxm.data.datasource.NetworkAddressSearchDataSource
import timber.log.Timber

interface LocationRepository {
    suspend fun getSuggestionLocation(suggestion: SearchSuggestion): Either<Failure, Location>
    suspend fun getUserCountryLocation(): Location?
}

class LocationRepositoryImpl(
    private val networkSearch: NetworkAddressSearchDataSource,
    private val cacheSearch: CacheAddressSearchDataSource,
    private val locationDataSource: LocationDataSource
) : LocationRepository {

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

    override suspend fun getUserCountryLocation(): Location? {
        return locationDataSource.getUserCountryLocation()
    }
}
