package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.TestConfig.failure
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.data.Failure
import com.weatherxm.data.NetworkSearchResults
import com.weatherxm.data.PublicDevice
import com.weatherxm.data.PublicHex
import com.weatherxm.data.datasource.DatabaseExplorerDataSource
import com.weatherxm.data.datasource.NetworkExplorerDataSource
import com.weatherxm.data.repository.ExplorerRepositoryImpl.Companion.EXCLUDE_PLACES
import com.weatherxm.ui.explorer.SearchResult
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.core.spec.style.scopes.BehaviorSpecWhenContainerScope
import io.kotest.matchers.shouldBe
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk

class ExplorerRepositoryTest : BehaviorSpec({
    lateinit var networkSource: NetworkExplorerDataSource
    lateinit var databaseSource: DatabaseExplorerDataSource
    lateinit var repo: ExplorerRepositoryImpl

    val cells = mockk<List<PublicHex>>()
    val cellIndex = "cellIndex"
    val deviceId = "deviceId"
    val cellDevices = mockk<List<PublicDevice>>()
    val cellDevice = mockk<PublicDevice>()
    val query = "query"
    val exclude = EXCLUDE_PLACES
    val searchResults = mockk<NetworkSearchResults>()
    val recentSearches = listOf<SearchResult>()
    val recentSearchesMaxSize = listOf<SearchResult>(
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk()
    )
    val searchResult = mockk<SearchResult>()

    beforeContainer {
        networkSource = mockk<NetworkExplorerDataSource>()
        databaseSource = mockk<DatabaseExplorerDataSource>()
        repo = ExplorerRepositoryImpl(networkSource, databaseSource)
        coJustRun { databaseSource.setRecentSearch(searchResult) }
        coJustRun { databaseSource.deleteOutOfLimitRecents() }
    }

    suspend fun BehaviorSpecWhenContainerScope.testNetworkSearch(
        function: suspend () -> Either<Failure, NetworkSearchResults>
    ) {
        and("the data source returns the search results") {
            coMockEitherRight({ function() }, searchResults)
            then("return the search results") {
                function().isSuccess(searchResults)
            }
        }
        and("the data source returns a failure") {
            coMockEitherLeft({ function() }, failure)
            then("return that failure") {
                function() shouldBe Either.Left(failure)
            }
        }
    }

    given("A data source providing a list of cells") {
        When("the data source returns a list of cells") {
            coMockEitherRight({ networkSource.getCells() }, cells)
            then("return that list of cells") {
                repo.getCells().isSuccess(cells)
            }
        }
        When("the data source returns a failure") {
            coMockEitherLeft({ networkSource.getCells() }, failure)
            then("return that failure") {
                repo.getCells() shouldBe Either.Left(failure)
            }
        }
    }

    given("A cell index") {
        and("A data source providing a list of devices for that cell") {
            When("the data source returns a list of devices") {
                coMockEitherRight({ networkSource.getCellDevices(cellIndex) }, cellDevices)
                then("return that list of devices") {
                    repo.getCellDevices(cellIndex).isSuccess(cellDevices)
                }
            }
            When("the data source returns a failure") {
                coMockEitherLeft({ networkSource.getCellDevices(cellIndex) }, failure)
                then("return that failure") {
                    repo.getCellDevices(cellIndex) shouldBe Either.Left(failure)
                }
            }
        }
        and("a device ID") {
            and("a data source providing the cell device") {
                When("the data source returns the cell device") {
                    coMockEitherRight(
                        { networkSource.getCellDevice(cellIndex, deviceId) },
                        cellDevice
                    )
                    then("return that cell device") {
                        repo.getCellDevice(cellIndex, deviceId).isSuccess(cellDevice)
                    }
                }
                When("the data source returns a failure") {
                    coMockEitherLeft(
                        { networkSource.getCellDevice(cellIndex, deviceId) },
                        failure
                    )
                    then("return that failure") {
                        repo.getCellDevice(cellIndex, deviceId) shouldBe Either.Left(failure)
                    }
                }
            }
        }
    }

    context("Perform network search related actions") {
        given("A query") {
            When("exact is null") {
                and("exclude is null") {
                    testNetworkSearch { networkSource.networkSearch(query) }
                }
                and("exclude is not null") {
                    testNetworkSearch { networkSource.networkSearch(query, exclude = exclude) }
                }
            }
            When("exact is true") {
                and("exclude is null") {
                    testNetworkSearch { networkSource.networkSearch(query, exact = true) }
                }
                and("exclude is not null") {
                    testNetworkSearch {
                        networkSource.networkSearch(query, exact = true, exclude = exclude)
                    }
                }
            }
            When("exact is false") {
                and("exclude is null") {
                    and("exclude is null") {
                        testNetworkSearch { networkSource.networkSearch(query, exact = false) }
                    }
                }
                and("exclude is not null") {
                    and("exclude is not null") {
                        testNetworkSearch {
                            networkSource.networkSearch(query, exact = false, exclude = exclude)
                        }
                    }
                }
            }
        }
        given("A list of recent searches") {
            When("the data source returns a list of recent searches") {
                coMockEitherRight({ databaseSource.getRecentSearches() }, recentSearches)
                then("return that list of recent searches") {
                    repo.getRecentSearches().isSuccess(recentSearches)
                }
            }
            When("the data source returns a failure") {
                coMockEitherLeft({ databaseSource.getRecentSearches() }, failure)
                then("return that failure") {
                    repo.getRecentSearches() shouldBe Either.Left(failure)
                }
            }
            When("We set a new SearchResult in recent searches") {
                and("the list of recent searches is not full") {
                    coMockEitherRight({ databaseSource.getRecentSearches() }, recentSearches)
                    then("set the new search result") {
                        repo.setRecentSearch(searchResult)
                        coVerify(exactly = 1) { databaseSource.setRecentSearch(searchResult) }
                    }
                }
                and("the list of recent searches is full") {
                    coMockEitherRight({ databaseSource.getRecentSearches() }, recentSearchesMaxSize)
                    then("set the new search result") {
                        repo.setRecentSearch(searchResult)
                        coVerify(exactly = 1) { databaseSource.setRecentSearch(searchResult) }
                    }
                    then("delete the recent which is out of limit") {
                        coVerify(exactly = 1) { databaseSource.deleteOutOfLimitRecents() }
                    }
                }
            }
        }
    }

})
