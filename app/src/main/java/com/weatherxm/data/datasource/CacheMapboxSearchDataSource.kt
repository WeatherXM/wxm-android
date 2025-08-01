package com.weatherxm.data.datasource

import arrow.core.Either
import com.mapbox.search.result.SearchSuggestion
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.Location
import com.weatherxm.data.services.CacheService

class CacheMapboxSearchDataSource(
    private val cacheService: CacheService
) : MapboxSearchDataSource {

    override suspend fun getSearchSuggestions(
        query: String,
        countryCode: String?
    ): Either<Failure, List<SearchSuggestion>> {
        return cacheService.getSearchSuggestions(query)
    }

    override suspend fun setSearchSuggestions(
        query: String,
        suggestions: List<SearchSuggestion>
    ) {
        cacheService.setSearchSuggestions(query, suggestions)
    }

    override suspend fun getSuggestionLocation(
        suggestion: SearchSuggestion
    ): Either<Failure, Location> {
        return cacheService.getSuggestionLocation(suggestion)
    }

    override suspend fun setSuggestionLocation(
        suggestion: SearchSuggestion,
        location: Location
    ) {
        cacheService.setSuggestionLocation(suggestion, location)
    }
}
