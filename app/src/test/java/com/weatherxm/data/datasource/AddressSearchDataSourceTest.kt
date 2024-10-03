package com.weatherxm.data.datasource

import com.mapbox.search.SearchEngine
import com.mapbox.search.SearchOptions
import com.mapbox.search.SearchSelectionCallback
import com.mapbox.search.SearchSuggestionsCallback
import com.mapbox.search.SelectOptions
import com.mapbox.search.common.AsyncOperationTask
import com.mapbox.search.common.IsoCountryCode
import com.mapbox.search.common.SearchCancellationException
import com.mapbox.search.result.SearchResult
import com.mapbox.search.result.SearchSuggestion
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.TestUtils.testGetFromCache
import com.weatherxm.TestUtils.testThrowNotImplemented
import com.weatherxm.data.datasource.NetworkAddressSearchDataSource.Companion.SEARCH_DEBOUNCE_MILLIS
import com.weatherxm.data.datasource.NetworkAddressSearchDataSource.Companion.SEARCH_LANGUAGES
import com.weatherxm.data.datasource.NetworkAddressSearchDataSource.Companion.SEARCH_LIMIT
import com.weatherxm.data.datasource.NetworkAddressSearchDataSource.Companion.SEARCH_TYPES
import com.weatherxm.data.models.CancellationError
import com.weatherxm.data.models.Location
import com.weatherxm.data.models.MapBoxError
import com.weatherxm.data.services.CacheService
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.koin.test.KoinTest

class AddressSearchDataSourceTest : KoinTest, BehaviorSpec({
    val searchEngine = mockk<SearchEngine>()
    val cache = mockk<CacheService>()
    val cacheSource = CacheAddressSearchDataSource(cache)
    val networkSource = NetworkAddressSearchDataSource(searchEngine)

    val query = "query"
    val countryCode = "GR"
    val location = Location(0.0, 0.0)
    val searchSuggestion = mockk<SearchSuggestion>()
    val searchSuggestions = listOf(searchSuggestion)
    val searchOptions = SearchOptions.Builder()
        .limit(SEARCH_LIMIT)
        .languages(SEARCH_LANGUAGES)
        .requestDebounce(SEARCH_DEBOUNCE_MILLIS)
        .types(SEARCH_TYPES)
        .countries(IsoCountryCode(countryCode))
        .build()

    val searchSuggestionsSlot = slot<SearchSuggestionsCallback>()
    val searchSelectionSlot = slot<SearchSelectionCallback>()
    val searchResult = mockk<SearchResult>().apply {
        coEvery { coordinate.latitude() } returns location.lat
        coEvery { coordinate.longitude() } returns location.lon
    }


    beforeSpec {
        coJustRun { cache.setSearchSuggestions(query, searchSuggestions) }
        coJustRun { cache.setSuggestionLocation(searchSuggestion, location) }
    }

    context("Get search suggestions by query and country code") {
        When("Using the Cache Source") {
            testGetFromCache(
                "search suggestions",
                searchSuggestions,
                mockFunction = { cache.getSearchSuggestions(query) },
                runFunction = { cacheSource.getSearchSuggestions(query, countryCode) }
            )
        }
        When("Using the Network Source") {
            and("the response is a success") {
                coEvery {
                    searchEngine.search(query, searchOptions, capture(searchSuggestionsSlot))
                }.answers {
                    searchSuggestionsSlot.captured.onSuggestions(searchSuggestions, mockk())
                    AsyncOperationTask.COMPLETED
                }
                then("return the Search Suggestions") {
                    networkSource.getSearchSuggestions(query, countryCode)
                        .isSuccess(searchSuggestions)
                }
            }
            and("the response is a failure") {
                and("the exception is a SearchCancellationException") {
                    coEvery {
                        searchEngine.search(query, searchOptions, capture(searchSuggestionsSlot))
                    }.answers {
                        searchSuggestionsSlot.captured.onError(SearchCancellationException(""))
                        AsyncOperationTask.COMPLETED
                    }
                    then("return CancellationError") {
                        networkSource.getSearchSuggestions(query, countryCode).leftOrNull()
                            .shouldBeTypeOf<CancellationError>()
                    }
                }
                and("the exception is NOT a SearchCancellationException") {
                    coEvery {
                        searchEngine.search(query, searchOptions, capture(searchSuggestionsSlot))
                    }.answers {
                        searchSuggestionsSlot.captured.onError(Exception())
                        AsyncOperationTask.COMPLETED
                    }
                    then("return GeocodingError") {
                        networkSource.getSearchSuggestions(query, countryCode).leftOrNull()
                            .shouldBeTypeOf<MapBoxError.GeocodingError>()
                    }
                }
            }
        }
    }

    context("Get the location of a search suggestion") {
        When("Using the Cache Source") {
            testGetFromCache(
                "location of the search suggestion",
                location,
                mockFunction = { cache.getSuggestionLocation(searchSuggestion) },
                runFunction = { cacheSource.getSuggestionLocation(searchSuggestion) }
            )
        }
        When("Using the Network Source") {
            and("the response is a success") {
                coEvery {
                    searchEngine.select(
                        searchSuggestion,
                        SelectOptions(false),
                        capture(searchSelectionSlot)
                    )
                }.answers {
                    searchSelectionSlot.captured.onResult(searchSuggestion, searchResult, mockk())
                    AsyncOperationTask.COMPLETED
                }
                then("return the Location") {
                    networkSource.getSuggestionLocation(searchSuggestion).isSuccess(location)
                }
            }
            and("the response is a failure") {
                coEvery {
                    searchEngine.select(
                        searchSuggestion,
                        SelectOptions(false),
                        capture(searchSelectionSlot)
                    )
                }.answers {
                    searchSelectionSlot.captured.onError(Exception())
                    AsyncOperationTask.COMPLETED
                }
                then("return SuggestionLocationError") {
                    networkSource.getSuggestionLocation(searchSuggestion).leftOrNull()
                        .shouldBeTypeOf<MapBoxError.SuggestionLocationError>()
                }
            }
        }
    }

    context("Set search suggestions") {
        When("Using the Cache Source") {
            then("save the search suggestions in the cache") {
                cacheSource.setSearchSuggestions(query, searchSuggestions)
                verify(exactly = 1) { cache.setSearchSuggestions(query, searchSuggestions) }
            }
        }
        When("Using the Network Source") {
            testThrowNotImplemented { networkSource.setSearchSuggestions(query, searchSuggestions) }
        }
    }

    context("Set a location of a search suggestion") {
        When("Using the Cache Source") {
            then("save the location in the cache") {
                cacheSource.setSuggestionLocation(searchSuggestion, location)
                verify(exactly = 1) { cache.setSuggestionLocation(searchSuggestion, location) }
            }
        }
        When("Using the Network Source") {
            testThrowNotImplemented {
                networkSource.setSuggestionLocation(searchSuggestion, location)
            }
        }
    }
})
