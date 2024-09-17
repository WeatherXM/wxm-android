package com.weatherxm.usecases

import com.mapbox.geojson.Point
import com.mapbox.search.result.SearchAddress
import com.mapbox.search.result.SearchSuggestion
import com.weatherxm.TestConfig.failure
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isError
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.data.CancellationError
import com.weatherxm.data.Device
import com.weatherxm.data.Location
import com.weatherxm.data.MapBoxError.ReverseGeocodingError
import com.weatherxm.data.repository.AddressRepository
import com.weatherxm.data.repository.DeviceRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk

class EditLocationUseCaseTest : BehaviorSpec({
    val addressRepository = mockk<AddressRepository>()
    val deviceRepository = mockk<DeviceRepository>()
    val usecase = EditLocationUseCaseImpl(addressRepository, deviceRepository)

    val query = "query"
    val deviceId = "deviceId"
    val lat = 0.0
    val lon = 0.0
    val suggestion = mockk<SearchSuggestion>()
    val point = mockk<Point>()
    val emptySearchAddress = SearchAddress()
    val searchAddress = SearchAddress(
        "1090",
        "6th Avenue",
        "Garment District",
        "0",
        "10036",
        "New York",
        "New York County",
        "New York",
        "United States"
    )
    val device = Device.empty()

    context("Get search suggestions from a query") {
        given("A repository providing the search suggestions") {
            When("it's a success") {
                then("return the search suggestions") {
                    coMockEitherRight(
                        { addressRepository.getSearchSuggestions(query) },
                        listOf(suggestion)
                    )
                    usecase.getSearchSuggestions(query).isSuccess(listOf(suggestion))
                }
            }
            When("it's a failure") {
                and("it's a cancellation error") {
                    coMockEitherLeft(
                        { addressRepository.getSearchSuggestions(query) },
                        CancellationError
                    )
                    then("return success with an empty list") {
                        usecase.getSearchSuggestions(query).isSuccess(emptyList())
                    }
                }
                and("it's not a cancellation error") {
                    coMockEitherLeft({ addressRepository.getSearchSuggestions(query) }, failure)
                    then("return the failure") {
                        usecase.getSearchSuggestions(query).isError()
                    }
                }
            }
        }
    }

    context("Get if a location from a search suggestion") {
        given("A repository providing the location") {
            When("it's a success") {
                then("return the location") {
                    coMockEitherRight(
                        { addressRepository.getSuggestionLocation(suggestion) },
                        Location(lat, lon)
                    )
                    usecase.getSuggestionLocation(suggestion).isSuccess(Location(lat, lon))
                }
            }
            When("it's a failure") {
                then("return the failure") {
                    coMockEitherLeft(
                        { addressRepository.getSuggestionLocation(suggestion) },
                        failure
                    )
                    usecase.getSuggestionLocation(suggestion).isError()
                }
            }
        }
    }

    context("Get address from a point") {
        given("A repository providing the address") {
            When("it's a success") {
                and("the address format is invalid") {
                    then("return a SearchResultAddressFormatError") {
                        coMockEitherRight(
                            { addressRepository.getAddressFromPoint(point) },
                            emptySearchAddress
                        )
                        val failure = usecase.getAddressFromPoint(point).leftOrNull()
                        (failure is
                            ReverseGeocodingError.SearchResultAddressFormatError) shouldBe true
                    }
                }
                and("the address format is valid") {
                    then("return the address") {
                        coMockEitherRight(
                            { addressRepository.getAddressFromPoint(point) },
                            searchAddress
                        )
                        usecase.getAddressFromPoint(point)
                            .isSuccess("1090 6th Avenue, New York, New York")
                    }
                }
            }
            When("it's a failure") {
                then("return the failure") {
                    coMockEitherLeft({ addressRepository.getAddressFromPoint(point) }, failure)
                    usecase.getAddressFromPoint(point).isError()
                }
            }
        }
    }

    context("Set a new location for a device") {
        given("A repository providing the SET mechanism") {
            When("it's a success") {
                then("return the UIDevice") {
                    coMockEitherRight({ deviceRepository.setLocation(deviceId, lat, lon) }, device)
                    usecase.setLocation(deviceId, lat, lon).isSuccess(device.toUIDevice())
                }
            }
            When("it's a failure") {
                then("return the failure") {
                    coMockEitherLeft({ deviceRepository.setLocation(deviceId, lat, lon) }, failure)
                    usecase.setLocation(deviceId, lat, lon).isError()
                }
            }
        }
    }
})
