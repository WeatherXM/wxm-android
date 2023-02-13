package com.weatherxm.data.datasource


import android.location.Location
import arrow.core.Either
import com.mapbox.geojson.Point
import com.mapbox.search.QueryType
import com.mapbox.search.ResponseInfo
import com.mapbox.search.SearchEngine
import com.mapbox.search.SearchOptions
import com.mapbox.search.SearchSelectionCallback
import com.mapbox.search.SearchSuggestionsCallback
import com.mapbox.search.SelectOptions
import com.mapbox.search.common.IsoCountryCode
import com.mapbox.search.common.IsoLanguageCode
import com.mapbox.search.common.SearchCancellationException
import com.mapbox.search.result.SearchResult
import com.mapbox.search.result.SearchSuggestion
import com.weatherxm.data.CancellationError
import com.weatherxm.data.Failure
import com.weatherxm.data.MapBoxError
import timber.log.Timber
import kotlin.coroutines.suspendCoroutine

/**
 * Search address network cache data source.
 */
class NetworkAddressSearchDataSource(
    private val mapBoxSearchEngine: SearchEngine
) : AddressSearchDataSource {

    companion object {
        /**
         * Limit of search results
         */
        private const val SEARCH_LIMIT = 10
        private val SEARCH_LANGUAGES = mutableListOf(IsoLanguageCode.ENGLISH)
        private const val SEARCH_DEBOUNCE_MILLIS = 500
        private val SEARCH_TYPES = mutableListOf(
            QueryType.ADDRESS,
            QueryType.NEIGHBORHOOD,
            QueryType.DISTRICT,
            QueryType.LOCALITY,
            QueryType.PLACE,
            QueryType.POSTCODE
        )
    }

    override suspend fun getSearchSuggestions(
        query: String,
        countryCode: String?
    ): Either<Failure, List<SearchSuggestion>> {
        return suspendCoroutine { continuation ->
            val searchOptionsBuilder = SearchOptions.Builder()
                .limit(SEARCH_LIMIT)
                .languages(SEARCH_LANGUAGES)
                .requestDebounce(SEARCH_DEBOUNCE_MILLIS)
                .types(SEARCH_TYPES)

            countryCode?.let {
                searchOptionsBuilder.countries(IsoCountryCode(countryCode))
            }

            mapBoxSearchEngine.search(
                query,
                searchOptionsBuilder.build(),
                object : SearchSuggestionsCallback {
                    override fun onSuggestions(
                        suggestions: List<SearchSuggestion>,
                        responseInfo: ResponseInfo
                    ) {
                        continuation.resumeWith(Result.success(Either.Right(suggestions)))
                    }

                    override fun onError(e: Exception) {
                        if (e is SearchCancellationException) {
                            Timber.d("Search was cancelled due to new query [query=$query]")
                            continuation.resumeWith(Result.success(Either.Left(CancellationError)))
                        } else {
                            Timber.w(e, "Geocoding failure [query=$query]")
                            continuation.resumeWith(
                                Result.success(Either.Left(MapBoxError.GeocodingError))
                            )
                        }
                    }
                }
            )
        }
    }

    override suspend fun setSearchSuggestions(
        query: String,
        suggestions: List<SearchSuggestion>
    ) {
        throw NotImplementedError()
    }

    override suspend fun getSuggestionLocation(
        suggestion: SearchSuggestion
    ): Either<Failure, Location> {
        return suspendCoroutine { continuation ->
            mapBoxSearchEngine.select(
                suggestion,
                SelectOptions(false),
                object : SearchSelectionCallback {
                    override fun onResult(
                        suggestion: SearchSuggestion,
                        result: SearchResult,
                        responseInfo: ResponseInfo
                    ) {
                        continuation.resumeWith(
                            Result.success(Either.Right(result.coordinate.toLocation()))
                        )
                    }

                    override fun onError(e: Exception) {
                        Timber.w(e, "Suggestion location search error.")
                        continuation.resumeWith(
                            Result.success(Either.Left(MapBoxError.SuggestionLocationError))
                        )
                    }

                    override fun onSuggestions(
                        suggestions: List<SearchSuggestion>,
                        responseInfo: ResponseInfo
                    ) {
                        /* Do nothing */
                    }

                    override fun onCategoryResult(
                        suggestion: SearchSuggestion,
                        results: List<SearchResult>,
                        responseInfo: ResponseInfo
                    ) {
                        /* Do nothing */
                    }
                })
        }
    }

    override suspend fun setSuggestionLocation(suggestion: SearchSuggestion, location: Location) {
        throw NotImplementedError()
    }

    private fun Point.toLocation(): Location {
        return Location("Search").apply {
            latitude = latitude()
            longitude = longitude()
        }
    }
}
