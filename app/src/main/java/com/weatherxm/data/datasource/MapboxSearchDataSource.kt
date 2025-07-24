package com.weatherxm.data.datasource

import arrow.core.Either
import com.mapbox.search.result.SearchSuggestion
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.Location

interface MapboxSearchDataSource {

    suspend fun getSearchSuggestions(
        query: String,
        countryCode: String? = null
    ): Either<Failure, List<SearchSuggestion>>

    suspend fun setSearchSuggestions(query: String, suggestions: List<SearchSuggestion>)

    suspend fun getSuggestionLocation(suggestion: SearchSuggestion): Either<Failure, Location>

    suspend fun setSuggestionLocation(suggestion: SearchSuggestion, location: Location)
}
