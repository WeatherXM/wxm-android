package com.weatherxm.data.datasource


import android.location.Location
import androidx.collection.ArrayMap
import arrow.core.Either
import arrow.core.flatMap
import arrow.core.rightIfNotNull
import com.mapbox.search.result.SearchSuggestion
import com.weatherxm.data.DataError
import com.weatherxm.data.Failure

/**
 * Search address in-memory cache data source.
 */
class CacheAddressSearchDataSource : AddressSearchDataSource {

    private var suggestions: ArrayMap<String, List<SearchSuggestion>> = ArrayMap()
    private var locations: ArrayMap<String, Location> = ArrayMap()

    override suspend fun getSearchSuggestions(
        query: String,
        countryCode: String?
    ): Either<Failure, List<SearchSuggestion>> {
        return suggestions[query].rightIfNotNull {
            DataError.CacheMissError
        }.flatMap {
            Either.Right(it)
        }
    }

    override suspend fun setSearchSuggestions(
        query: String,
        suggestions: List<SearchSuggestion>
    ) {
        this.suggestions[query] = suggestions
    }

    override suspend fun getSuggestionLocation(
        suggestion: SearchSuggestion
    ): Either<Failure, Location> {
        return locations[suggestion.id].rightIfNotNull {
            DataError.CacheMissError
        }.flatMap {
            Either.Right(it)
        }
    }

    override suspend fun setSuggestionLocation(
        suggestion: SearchSuggestion,
        location: Location
    ) {
        locations[suggestion.id] = location
    }

    override suspend fun clear() {
        suggestions.clear()
        locations.clear()
    }
}
