package com.weatherxm.data.repository

import com.mapbox.search.result.SearchSuggestion
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.data.models.Location
import com.weatherxm.data.datasource.CacheAddressSearchDataSource
import com.weatherxm.data.datasource.LocationDataSource
import com.weatherxm.data.datasource.NetworkAddressSearchDataSource
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk

class LocationRepositoryTest : BehaviorSpec({
    val networkSearchSource = mockk<NetworkAddressSearchDataSource>()
    val cacheSearchSource = mockk<CacheAddressSearchDataSource>()
    val source = mockk<LocationDataSource>()
    val repo = LocationRepositoryImpl(networkSearchSource, cacheSearchSource, source)

    val location = mockk<Location>()
    val searchSuggestion = mockk<SearchSuggestion>()

    beforeSpec {
        coJustRun { cacheSearchSource.setSuggestionLocation(searchSuggestion, location) }
    }

    context("Get Location from a Search Suggestion") {
        When("it's in the cache") {
            coMockEitherRight(
                { cacheSearchSource.getSuggestionLocation(searchSuggestion) },
                location
            )
            then("return the location") {
                repo.getSuggestionLocation(searchSuggestion).isSuccess(location)
            }
        }
        When("it's not in the cache") {
            coMockEitherLeft(
                { cacheSearchSource.getSuggestionLocation(searchSuggestion) }, mockk()
            )
            and("we can get it from the network") {
                coMockEitherRight(
                    { networkSearchSource.getSuggestionLocation(searchSuggestion) },
                    location
                )
                then("return the location") {
                    repo.getSuggestionLocation(searchSuggestion).isSuccess(location)
                }
                then("save it in the cache") {
                    coVerify(exactly = 1) {
                        cacheSearchSource.setSuggestionLocation(searchSuggestion, location)
                    }
                }
            }
        }
    }

    context("Get a user's country's location") {
        given("that location") {
            When("it's null") {
                coEvery { source.getUserCountryLocation() } returns null
                then("return null") {
                    repo.getUserCountryLocation() shouldBe null
                }
            }
            When("It's not null") {
                coEvery { source.getUserCountryLocation() } returns location
                then("return the location") {
                    repo.getUserCountryLocation() shouldBe location
                }
            }
        }
    }
})
