package com.weatherxm.data.datasource

import com.haroldadmin.cnradapter.NetworkResponse
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.TestUtils.retrofitResponse
import com.weatherxm.TestUtils.testNetworkCall
import com.weatherxm.TestUtils.testThrowNotImplemented
import com.weatherxm.data.database.dao.NetworkSearchRecentDao
import com.weatherxm.data.database.entities.NetworkSearchRecent
import com.weatherxm.data.models.Bundle
import com.weatherxm.data.models.DataError
import com.weatherxm.data.models.Location
import com.weatherxm.data.models.NetworkSearchResults
import com.weatherxm.data.models.PublicDevice
import com.weatherxm.data.models.PublicHex
import com.weatherxm.data.network.ApiService
import com.weatherxm.data.network.ErrorResponse
import com.weatherxm.data.repository.ExplorerRepositoryImpl.Companion.EXCLUDE_PLACES
import com.weatherxm.data.repository.ExplorerRepositoryImpl.Companion.RECENTS_MAX_ENTRIES
import com.weatherxm.ui.home.explorer.SearchResult
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.coJustRun
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.ZonedDateTime
import java.util.Date

class ExplorerDataSourceTest : BehaviorSpec({
    val apiService = mockk<ApiService>()
    val dao = mockk<NetworkSearchRecentDao>()
    val networkSource = NetworkExplorerDataSource(apiService)
    val databaseSource = DatabaseExplorerDataSource(dao)

    val index = "index"
    val deviceId = "deviceId"
    val query = "query"
    val exact = false
    val exclude = EXCLUDE_PLACES

    val publicCells = listOf<PublicHex>()
    val cellsResponse = NetworkResponse.Success<List<PublicHex>, ErrorResponse>(
        publicCells,
        retrofitResponse(publicCells)
    )

    val cellDevices = listOf<PublicDevice>()
    val cellDevicesResponse = NetworkResponse.Success<List<PublicDevice>, ErrorResponse>(
        cellDevices,
        retrofitResponse(cellDevices)
    )

    val publicDevice = mockk<PublicDevice>()
    val publicDeviceResponse = NetworkResponse.Success<PublicDevice, ErrorResponse>(
        publicDevice,
        retrofitResponse(publicDevice)
    )

    val networkSearch = mockk<NetworkSearchResults>()
    val networkSearchResponse = NetworkResponse.Success<NetworkSearchResults, ErrorResponse>(
        networkSearch,
        retrofitResponse(networkSearch)
    )

    val networkSearchRecent = NetworkSearchRecent(
        "name",
        0.0,
        0.0,
        "place",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null
    )
    val networkSearchRecents = mutableListOf<NetworkSearchRecent>().apply {
        repeat(RECENTS_MAX_ENTRIES - 1) {
            networkSearchRecent.updatedAt = Date.from(ZonedDateTime.now().toInstant())
            add(networkSearchRecent)
        }
    }

    val noNameSearchResult = SearchResult(null, null, null, null, null, null)
    val noCenterSearchResult = SearchResult("", null, null, null, null, null)
    val validSearchResult = SearchResult(
        "name",
        Location.empty(),
        "place",
        Bundle(null, null, null, null, null, null),
        null,
        null
    )

    beforeSpec {
        coJustRun { dao.deleteAll() }
        coJustRun { dao.deleteOutOfLimitRecents(any()) }
        coJustRun { dao.insert(any()) }
    }

    context("Get public cells") {
        given("A Network and a Database Source providing the public cells") {
            When("Using the Network Source") {
                testNetworkCall(
                    "Public Cells",
                    publicCells,
                    cellsResponse,
                    mockFunction = { apiService.getCells() },
                    runFunction = { networkSource.getCells() }
                )
            }
            When("Using the Database Source") {
                testThrowNotImplemented { databaseSource.getCells() }
            }
        }
    }

    context("Get devices of a cell") {
        given("A Network and a Database Source providing the devices of a cell") {
            When("Using the Network Source") {
                testNetworkCall(
                    "devices of the cell",
                    cellDevices,
                    cellDevicesResponse,
                    mockFunction = { apiService.getCellDevices(index) },
                    runFunction = { networkSource.getCellDevices(index) }
                )
            }
            When("Using the Database Source") {
                testThrowNotImplemented { databaseSource.getCellDevices(index) }
            }
        }
    }

    context("Get a specific device of a cell") {
        given("A Network and a Database Source providing that device") {
            When("Using the Network Source") {
                testNetworkCall(
                    "device",
                    publicDevice,
                    publicDeviceResponse,
                    mockFunction = { apiService.getCellDevice(index, deviceId) },
                    runFunction = { networkSource.getCellDevice(index, deviceId) }
                )
            }
            When("Using the Database Source") {
                testThrowNotImplemented { databaseSource.getCellDevice(index, deviceId) }
            }
        }
    }

    context("Perform a network search") {
        given("A Network and a Database Source providing the results of the network search") {
            When("Using the Network Source") {
                testNetworkCall(
                    "results of the network search",
                    networkSearch,
                    networkSearchResponse,
                    mockFunction = { apiService.networkSearch(query, exact, exclude) },
                    runFunction = { networkSource.networkSearch(query, exact, exclude) }
                )
            }
            When("Using the Database Source") {
                testThrowNotImplemented { databaseSource.networkSearch(query, exact, exclude) }
            }
        }
    }

    context("Delete all recent searches") {
        given("A Network and a Database Source providing the DELETE mechanism") {
            When("Using the Network Source") {
                testThrowNotImplemented { networkSource.deleteAll() }
            }
            When("Using the Database Source") {
                then("Should delete all recent searches") {
                    databaseSource.deleteAll()
                    verify(exactly = 1) { dao.deleteAll() }
                }
            }
        }
    }

    context("Delete any out of limit searches") {
        given("A Network and a Database Source providing the DELETE mechanism") {
            When("Using the Network Source") {
                testThrowNotImplemented { networkSource.deleteOutOfLimitRecents() }
            }
            When("Using the Database Source") {
                When("The saved searches are less than $RECENTS_MAX_ENTRIES") {
                    every { dao.getAll() } returns networkSearchRecents
                    then("Do nothing") {
                        databaseSource.deleteOutOfLimitRecents()
                        verify(exactly = 0) { dao.deleteOutOfLimitRecents(any()) }
                    }
                }
                When("The saved searches are equal to $RECENTS_MAX_ENTRIES") {
                    networkSearchRecents.add(networkSearchRecent)
                    then("delete the oldest ones") {
                        databaseSource.deleteOutOfLimitRecents()
                        verify(exactly = 1) { dao.deleteOutOfLimitRecents(any()) }
                    }
                }
                When("The saved searches are more than $RECENTS_MAX_ENTRIES") {
                    networkSearchRecents.add(networkSearchRecent)
                    then("delete the oldest ones") {
                        databaseSource.deleteOutOfLimitRecents()
                        verify(exactly = 2) { dao.deleteOutOfLimitRecents(any()) }
                    }
                }
            }
        }
    }

    context("Save recent search in database") {
        given("A Network and a Database Source implementing the SET mechanism") {
            When("Using the Network Source") {
                testThrowNotImplemented { networkSource.setRecentSearch(validSearchResult) }
            }
            When("Using the Database Source") {
                and("the search result has no name") {
                    then("return and do nothing") {
                        databaseSource.setRecentSearch(noNameSearchResult)
                        verify(exactly = 0) { dao.insert(any()) }
                    }
                }
                and("the search result has no center") {
                    then("return and do nothing") {
                        databaseSource.setRecentSearch(noCenterSearchResult)
                        verify(exactly = 0) { dao.insert(any()) }
                    }
                }
                and("none of the above applies and the search result is valid") {
                    then("save the search result") {
                        databaseSource.setRecentSearch(validSearchResult)
                        verify(exactly = 1) { dao.insert(any()) }
                    }
                }
            }
        }
    }

    context("Get recent searches in database") {
        given("A Network and a Database Source implementing the GET mechanism") {
            When("Using the Network Source") {
                testThrowNotImplemented { networkSource.getRecentSearches() }
            }
            When("Using the Database Source") {
                and("The Database is empty") {
                    every { dao.getAll() } returns emptyList()
                    then("return a DatabaseMissError") {
                        databaseSource.getRecentSearches().leftOrNull()
                            .shouldBeTypeOf<DataError.DatabaseMissError>()
                    }
                }
                and("database is not empty") {
                    every { dao.getAll() } returns listOf(networkSearchRecent)
                    then("return the recent searches") {
                        databaseSource.getRecentSearches().isSuccess(listOf(validSearchResult))
                    }
                }
            }
        }
    }
})
