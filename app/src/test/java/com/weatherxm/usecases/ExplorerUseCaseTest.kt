package com.weatherxm.usecases

import com.weatherxm.TestConfig.failure
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isError
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.data.models.DataError
import com.weatherxm.data.models.Location
import com.weatherxm.data.models.NetworkSearchAddressResult
import com.weatherxm.data.models.NetworkSearchDeviceResult
import com.weatherxm.data.models.NetworkSearchResults
import com.weatherxm.data.models.PublicDevice
import com.weatherxm.data.models.PublicHex
import com.weatherxm.data.repository.DeviceRepository
import com.weatherxm.data.repository.ExplorerRepository
import com.weatherxm.data.repository.ExplorerRepositoryImpl.Companion.EXCLUDE_PLACES
import com.weatherxm.data.repository.FollowRepository
import com.weatherxm.data.repository.LocationRepository
import com.weatherxm.ui.common.DeviceRelation
import com.weatherxm.ui.explorer.ExplorerData
import com.weatherxm.ui.explorer.SearchResult
import com.weatherxm.ui.explorer.UICell
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import java.time.ZonedDateTime

class ExplorerUseCaseTest : BehaviorSpec({
    val explorerRepo = mockk<ExplorerRepository>()
    val followRepo = mockk<FollowRepository>()
    val deviceRepo = mockk<DeviceRepository>()
    val locationRepo = mockk<LocationRepository>()
    val usecase = ExplorerUseCaseImpl(
        explorerRepo, followRepo, deviceRepo, locationRepo
    )

    val searchResult = SearchResult(null, null)
    val index = "index"
    val address = "address"
    val location = Location(0.0, 0.0)
    val publicHexes = mutableListOf(PublicHex(index, 0, location, emptyList()))
    val cell = UICell(index, location)

    val ownedDevice = PublicDevice(
        "ownedDevice",
        "owned",
        null,
        null,
        null,
        ZonedDateTime.now(),
        index,
        null,
        address,
        null
    )
    val followedDevice =
        PublicDevice(
            "followedDevice",
            "followed",
            null,
            null,
            null,
            null,
            index,
            null,
            address,
            null
        )
    val publicDevice =
        PublicDevice("publicDevice", "public", null, null, null, null, index, null, address, null)

    val ownedUiDevice = ownedDevice.toUIDevice().apply {
        this.cellCenter = location
        this.relation = DeviceRelation.OWNED
    }
    val followedUiDevice = followedDevice.toUIDevice().apply {
        this.cellCenter = location
        this.relation = DeviceRelation.FOLLOWED
    }
    val publicUiDevice = publicDevice.toUIDevice().apply {
        this.cellCenter = location
        this.relation = DeviceRelation.UNFOLLOWED
    }

    val query = "query"
    val exact = false
    val exclude = EXCLUDE_PLACES
    val emptyNetworkSearchResults = NetworkSearchResults(null, null)
    val networkSearchDeviceResult = NetworkSearchDeviceResult(null, null, null, null, null)
    val networkSearchAddressResult = NetworkSearchAddressResult(null, null, null)
    val networkSearchResults =
        NetworkSearchResults(listOf(networkSearchDeviceResult), listOf(networkSearchAddressResult))

    beforeSpec {
        coJustRun { explorerRepo.setRecentSearch(searchResult) }
        coEvery { deviceRepo.getUserDevicesIds() } returns listOf("ownedDevice")
        coEvery { followRepo.getFollowedDevicesIds() } returns listOf("followedDevice")
    }

    context("Get Cell Info") {
        given("A repository providing the cells") {
            When("it's a success") {
                and("the cell exists") {
                    coMockEitherRight({ explorerRepo.getCells() }, publicHexes)
                    then("return the cell") {
                        usecase.getCellInfo(index).isSuccess(cell)
                    }
                }
                and("the cell doesn't exist") {
                    coMockEitherRight({ explorerRepo.getCells() }, emptyList<PublicHex>())
                    then("return a CellNotFound failure") {
                        usecase.getCellInfo(index).leftOrNull()
                            .shouldBeTypeOf<DataError.CellNotFound>()
                    }
                }
            }
            When("it's a failure") {
                coMockEitherLeft({ explorerRepo.getCells() }, failure)
                then("return that failure") {
                    usecase.getCellInfo(index).isError()
                }
            }
        }
    }

    context("Get Cells") {
        given("A repository providing the cells") {
            When("it's a success") {
                coMockEitherRight({ explorerRepo.getCells() }, publicHexes)
                then("return ExplorerData") {
                    usecase.getCells().onRight {
                        it.shouldBeTypeOf<ExplorerData>()
                        it.publicHexes shouldBe publicHexes
                        it.polygonsToDraw shouldBe emptyList()
                    }
                }
            }
            When("it's a failure") {
                coMockEitherLeft({ explorerRepo.getCells() }, failure)
                then("return that failure") {
                    usecase.getCells().isError()
                }
            }
        }
    }

    context("Get Cell Devices") {
        given("A repository providing the cell devices") {
            When("it's a success") {
                coMockEitherRight(
                    { explorerRepo.getCellDevices(index) },
                    listOf(publicDevice, followedDevice, ownedDevice)
                )
                then("return a list of UIDevices sorted (by timestamps and then by name)") {
                    usecase.getCellDevices(cell)
                        .isSuccess(listOf(ownedUiDevice, followedUiDevice, publicUiDevice))
                }
            }
            When("it's a failure") {
                coMockEitherLeft({ explorerRepo.getCellDevices(index) }, failure)
                then("return that failure") {
                    usecase.getCellDevices(cell).isError()
                }
            }
        }
    }

    context("Get Cell Device") {
        given("A repository providing the device") {
            When("it's a success") {
                coMockEitherRight(
                    { explorerRepo.getCellDevice(index, ownedDevice.id) },
                    ownedDevice
                )
                then("return the UIDevice") {
                    ownedUiDevice.cellCenter = null
                    usecase.getCellDevice(index, ownedUiDevice.id).isSuccess(ownedUiDevice)
                }
            }
            When("it's a failure") {
                coMockEitherLeft({ explorerRepo.getCellDevice(index, ownedDevice.id) }, failure)
                then("return that failure") {
                    usecase.getCellDevice(index, ownedDevice.id).isError()
                }
            }
        }
    }

    context("Perform network search") {
        given("A repository accepting the query and providing the results") {
            When("it's a success") {
                and("the response contain no results (empty response)") {
                    coMockEitherRight(
                        { explorerRepo.networkSearch(query, exact, exclude) },
                        emptyNetworkSearchResults
                    )
                    then("return an empty list") {
                        usecase.networkSearch(query, exact, exclude).isSuccess(emptyList())
                    }
                }
                and("the response contain devices and addresses") {
                    coMockEitherRight(
                        { explorerRepo.networkSearch(query, exact, exclude) },
                        networkSearchResults
                    )
                    then("return a list of SearchResult") {
                        usecase.networkSearch(query, exact, exclude).isSuccess(
                            listOf(
                                SearchResult(null, null, relation = DeviceRelation.UNFOLLOWED),
                                SearchResult(null, null),
                            )
                        )
                    }
                }
            }
            When("it's a failure") {
                coMockEitherLeft({ explorerRepo.networkSearch(query, exact, exclude) }, failure)
                then("return that failure") {
                    usecase.networkSearch(query, exact, exclude).isError()
                }
            }
        }
    }

    context("Get/Set Recent Searches") {
        given("A repository providing the recent searches") {
            When("it's a success") {
                coMockEitherRight({ explorerRepo.getRecentSearches() }, listOf(searchResult))
                then("return them as a list of strings") {
                    usecase.getRecentSearches() shouldBe listOf(searchResult)
                }
            }
            When("it's a failure") {
                coMockEitherLeft({ explorerRepo.getRecentSearches() }, failure)
                then("return that failure") {
                    usecase.getRecentSearches() shouldBe emptyList()
                }
            }
        }
        given("A repository providing the SET mechanism for recent searches") {
            then("set he recent searches") {
                usecase.setRecentSearch(searchResult)
                coVerify(exactly = 1) { explorerRepo.setRecentSearch(searchResult) }
            }
        }
    }

    context("Get user country's location") {
        given("A repository providing the location") {
            coEvery { locationRepo.getUserCountryLocation() } returns location
            then("return the location") {
                usecase.getUserCountryLocation() shouldBe location
            }
        }
    }
})
