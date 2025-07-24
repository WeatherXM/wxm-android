package com.weatherxm.data.repository

import com.mapbox.geojson.Point
import com.mapbox.search.result.ResultAccuracy
import com.mapbox.search.result.SearchAddress
import com.mapbox.search.result.SearchResult
import com.mapbox.search.result.SearchSuggestion
import com.weatherxm.TestConfig.failure
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isError
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.data.datasource.CacheMapboxSearchDataSource
import com.weatherxm.data.datasource.NetworkMapboxSearchDataSource
import com.weatherxm.data.datasource.ReverseGeocodingDataSourceImpl
import com.weatherxm.data.models.CountryAndFrequencies
import com.weatherxm.data.models.Frequency
import com.weatherxm.data.models.Location
import com.weatherxm.data.models.MapBoxError.ReverseGeocodingError
import com.weatherxm.data.repository.GeoLocationRepositoryImpl.Companion.MAX_GEOCODING_DISTANCE_METERS
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.core.spec.style.scopes.BehaviorSpecWhenContainerScope
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk

class GeoLocationRepositoryTest : BehaviorSpec({
    val networkSource = mockk<ReverseGeocodingDataSourceImpl>()
    val networkAddressSearchSource = mockk<NetworkMapboxSearchDataSource>()
    val cacheSearchSource = mockk<CacheMapboxSearchDataSource>()
    val repo = GeoLocationRepositoryImpl(
        networkSource,
        networkAddressSearchSource,
        cacheSearchSource
    )

    val query = "query"
    val searchSuggestion = mockk<SearchSuggestion>()
    val searchSuggestions = listOf(searchSuggestion)
    val searchSuggestionsGlobal = listOf(searchSuggestion, searchSuggestion)
    val point = mockk<Point>()
    val searchResult = mockk<SearchResult>()
    val searchAddress = mockk<SearchAddress>()
    val country = "Greece"
    val location = Location.empty()
    val countryAndFrequencies = CountryAndFrequencies(country, Frequency.EU868, listOf(mockk()))

    beforeSpec {
        coJustRun { cacheSearchSource.setSuggestionLocation(searchSuggestion, location) }
        coJustRun { cacheSearchSource.setSearchSuggestions(query, searchSuggestions) }
        coJustRun { cacheSearchSource.setSearchSuggestions(query, searchSuggestionsGlobal) }
        every { networkSource.getUserCountry() } returns country
    }

    suspend fun BehaviorSpecWhenContainerScope.testGetAddressFromAccuratePoint() {
        When("distance meters is null") {
            every { searchResult.distanceMeters } returns null
            then("return failure SearchResultNotNearbyError") {
                repo.getAddressFromPoint(point).leftOrNull()
                    .shouldBeTypeOf<ReverseGeocodingError.SearchResultNotNearbyError>()
            }
        }
        When("distance meters is more than $MAX_GEOCODING_DISTANCE_METERS") {
            every {
                searchResult.distanceMeters
            } returns MAX_GEOCODING_DISTANCE_METERS + 1
            then("return failure SearchResultNotNearbyError") {
                repo.getAddressFromPoint(point).leftOrNull()
                    .shouldBeTypeOf<ReverseGeocodingError.SearchResultNotNearbyError>()
            }
        }
        When("distance meters is less than $MAX_GEOCODING_DISTANCE_METERS") {
            every {
                searchResult.distanceMeters
            } returns MAX_GEOCODING_DISTANCE_METERS - 1
            When("address is null") {
                every { searchResult.address } returns null
                then("return failure SearchResultNoAddressError") {
                    repo.getAddressFromPoint(point).leftOrNull()
                        .shouldBeTypeOf<ReverseGeocodingError.SearchResultNoAddressError>()
                }
            }
            When("address is not null") {
                every { searchResult.address } returns searchAddress
                then("return that success") {
                    repo.getAddressFromPoint(point).isSuccess(searchAddress)
                }
            }
        }
    }

    context("Get Search Suggestions from a query") {
        given("a query") {
            When("we have in cache the location of this search suggestion") {
                coMockEitherRight(
                    { cacheSearchSource.getSearchSuggestions(query) },
                    searchSuggestions
                )
                then("return these search suggestions") {
                    repo.getSearchSuggestions(query).isSuccess(searchSuggestions)
                }
            }
            When("we don't have in cache that location so we fetch it from network") {
                coMockEitherLeft(
                    { cacheSearchSource.getSearchSuggestions(query) },
                    failure
                )
                When("the response is a failure") {
                    coMockEitherLeft(
                        { networkAddressSearchSource.getSearchSuggestions(query, country) },
                        failure
                    )
                    then("return that failure") {
                        repo.getSearchSuggestions(query).isError()
                    }
                }
                When("the response is a success") {
                    When("country results are not empty") {
                        coMockEitherRight(
                            { networkAddressSearchSource.getSearchSuggestions(query, country) },
                            searchSuggestions
                        )
                        then("return these search suggestions") {
                            repo.getSearchSuggestions(query).isSuccess(searchSuggestions)
                        }
                        then("save these search suggestions in cache") {
                            coVerify(exactly = 1) {
                                cacheSearchSource.setSearchSuggestions(query, searchSuggestions)
                            }
                        }
                    }
                    When("country results are empty") {
                        coMockEitherRight(
                            { networkAddressSearchSource.getSearchSuggestions(query, country) },
                            emptyList<SearchSuggestion>()
                        )
                        and("search from Search Suggestions globally") {
                            When("the response is a failure") {
                                coMockEitherLeft(
                                    { networkAddressSearchSource.getSearchSuggestions(query) },
                                    failure
                                )
                                then("return that failure") {
                                    repo.getSearchSuggestions(query).isError()
                                }
                            }
                            When("the response is a success") {
                                coMockEitherRight(
                                    { networkAddressSearchSource.getSearchSuggestions(query) },
                                    searchSuggestionsGlobal
                                )
                                then("return these search suggestions") {
                                    repo.getSearchSuggestions(query)
                                        .isSuccess(searchSuggestionsGlobal)
                                }
                                then("save these search suggestions in cache") {
                                    coVerify(exactly = 1) {
                                        cacheSearchSource.setSearchSuggestions(
                                            query,
                                            searchSuggestionsGlobal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    context("Get location from a Search Suggestion") {
        given("a search suggestion") {
            When("we have in cache the location of this search suggestion") {
                coMockEitherRight(
                    { cacheSearchSource.getSuggestionLocation(searchSuggestion) },
                    location
                )
                then("return that location") {
                    repo.getSuggestionLocation(searchSuggestion).isSuccess(location)
                }
            }
            When("we don't have in cache that location so we fetch it from network") {
                coMockEitherLeft(
                    { cacheSearchSource.getSuggestionLocation(searchSuggestion) },
                    failure
                )
                When("the response is a failure") {
                    coMockEitherLeft(
                        { networkAddressSearchSource.getSuggestionLocation(searchSuggestion) },
                        failure
                    )
                    then("return that failure") {
                        repo.getSuggestionLocation(searchSuggestion).isError()
                    }
                }
                When("the response is a success") {
                    coMockEitherRight(
                        { networkAddressSearchSource.getSuggestionLocation(searchSuggestion) },
                        location
                    )
                    then("return that success") {
                        repo.getSuggestionLocation(searchSuggestion).isSuccess(location)
                    }
                    then("save this location in cache") {
                        coVerify(exactly = 1) {
                            cacheSearchSource.setSuggestionLocation(searchSuggestion, location)
                        }
                    }
                }
            }
        }
    }

    context("Get address from Point") {
        given("a response containing the address of that point") {
            When("the response is a failure") {
                coMockEitherLeft({ networkSource.getAddressFromPoint(point) }, failure)
                then("return that failure") {
                    repo.getAddressFromPoint(point).isError()
                }
            }
            When("the response is a success") {
                coMockEitherRight({ networkSource.getAddressFromPoint(point) }, searchResult)
                When("accuracy is not point or rooftop or street") {
                    every { searchResult.accuracy } returns ResultAccuracy.Approximate
                    then("return failure SearchResultNotAccurateError") {
                        repo.getAddressFromPoint(point).leftOrNull()
                            .shouldBeTypeOf<ReverseGeocodingError.SearchResultNotAccurateError>()
                    }
                }
                When("accuracy is point") {
                    every { searchResult.accuracy } returns ResultAccuracy.Point
                    testGetAddressFromAccuratePoint()
                }
                When("accuracy is street") {
                    every { searchResult.accuracy } returns ResultAccuracy.Street
                    testGetAddressFromAccuratePoint()
                }
                When("accuracy is rooftop") {
                    every { searchResult.accuracy } returns ResultAccuracy.Rooftop
                    testGetAddressFromAccuratePoint()
                }
            }
        }
    }

    context("Get Country and Frequencies from Location") {
        given("a response containing that info") {
            When("the response is a failure") {
                coMockEitherLeft({ networkSource.getCountryAndFrequencies(location) }, failure)
                then("return the default county and frequencies") {
                    repo.getCountryAndFrequencies(location) shouldBe CountryAndFrequencies.default()
                }
            }
            When("the response is a success") {
                coMockEitherRight(
                    { networkSource.getCountryAndFrequencies(location) },
                    countryAndFrequencies
                )
                then("return that info") {
                    repo.getCountryAndFrequencies(location) shouldBe countryAndFrequencies
                }
            }
        }
    }

    context("Get a user's country's location") {
        given("that location") {
            When("it's null") {
                coEvery { networkSource.getUserCountryLocation() } returns null
                then("return null") {
                    repo.getUserCountryLocation() shouldBe null
                }
            }
            When("It's not null") {
                coEvery { networkSource.getUserCountryLocation() } returns location
                then("return the location") {
                    repo.getUserCountryLocation() shouldBe location
                }
            }
        }
    }
})
