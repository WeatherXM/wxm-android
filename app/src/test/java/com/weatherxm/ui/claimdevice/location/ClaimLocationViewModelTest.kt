package com.weatherxm.ui.claimdevice.location

import com.mapbox.geojson.Point
import com.mapbox.search.result.SearchSuggestion
import com.weatherxm.TestConfig.failure
import com.weatherxm.TestConfig.resources
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.Location
import com.weatherxm.ui.InstantExecutorListener
import com.weatherxm.ui.common.DeviceType
import com.weatherxm.usecases.EditLocationUseCase
import com.weatherxm.util.LocationHelper
import com.weatherxm.util.Resources
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

@OptIn(ExperimentalCoroutinesApi::class)
class ClaimLocationViewModelTest : BehaviorSpec({
    val usecase = mockk<EditLocationUseCase>()
    val locationHelper = mockk<LocationHelper>()
    val analytics = mockk<AnalyticsWrapper>()
    lateinit var viewModel: ClaimLocationViewModel

    val deviceType = DeviceType.D1_WIFI
    val query = "query"
    val address = "address"
    val lat = 10.0
    val lon = 10.0
    val locationSlot = slot<(location: Location?) -> Unit>()
    val searchSuggestion = mockk<SearchSuggestion>()
    val point = mockk<Point>()

    listener(InstantExecutorListener())
    Dispatchers.setMain(StandardTestDispatcher())

    beforeSpec {
        startKoin {
            modules(
                module {
                    single<Resources> {
                        resources
                    }
                }
            )
        }
        justRun { analytics.trackEventFailure(any()) }

        viewModel = ClaimLocationViewModel(usecase, analytics, locationHelper)
    }

    context("SET a Device Type and then GET it") {
        given("a device type") {
            then("ensure that the current one is the default one") {
                viewModel.getDeviceType() shouldBe DeviceType.M5_WIFI
            }
            and("SET it") {
                viewModel.setDeviceType(deviceType)
                then("GET it and ensure it's set correctly") {
                    viewModel.getDeviceType() shouldBe deviceType
                }
            }
        }
    }

    context("SET an installation location and then GET it") {
        given("a claiming key") {
            then("ensure that the current one is the default one") {
                viewModel.getInstallationLocation() shouldBe Location(0.0, 0.0)
            }
            then("VALIDATE the new location to ensure it's correct") {
                viewModel.validateLocation(lat, lon) shouldBe true
            }
            and("SET it") {
                viewModel.setInstallationLocation(lat, lon)
                then("GET it and ensure it's set correctly") {
                    viewModel.getInstallationLocation() shouldBe Location(lat, lon)
                }
            }
        }
    }

    context("Get user's location") {
        given("a request to the user to provide it") {
            then("LiveData onRequestUserLocation should post the value true") {
                viewModel.onRequestUserLocation().value shouldBe false
                viewModel.requestUserLocation()
                viewModel.onRequestUserLocation().value shouldBe true
            }
            and("use location helper to get the user's location") {
                every {
                    locationHelper.getLocationAndThen(capture(locationSlot))
                } answers {
                    locationSlot.captured.invoke(Location(lat, lon))
                }
                viewModel.getLocation()
                viewModel.onMoveToLocation().value shouldBe Location(lat, lon)
            }
        }
    }

    context("Get the search suggestions from a query") {
        given("a usecase returning the search suggestions from the query") {
            When("it's a failure") {
                coMockEitherLeft({ usecase.getSearchSuggestions(query) }, failure)
                runTest { viewModel.getSearchSuggestions(query) }
                then("LiveData onSearchResults should post the value null") {
                    viewModel.onSearchResults().value shouldBe null
                }
            }
            When("it's a success") {
                coMockEitherRight({ usecase.getSearchSuggestions(query) }, listOf(searchSuggestion))
                runTest { viewModel.getSearchSuggestions(query) }
                then("LiveData onMoveToLocation posts the location") {
                    viewModel.onSearchResults().value shouldBe listOf(searchSuggestion)
                }
            }
        }
    }

    context("Get Location from a search suggestion") {
        given("a usecase returning the location of the search suggestion") {
            When("it's a failure") {
                coMockEitherLeft({ usecase.getSuggestionLocation(searchSuggestion) }, failure)
                runTest { viewModel.getLocationFromSearchSuggestion(searchSuggestion) }
                then("Do nothing just track the event's failure in analytics") {
                    verify(exactly = 1) { analytics.trackEventFailure(any()) }
                }
            }
            When("it's a success") {
                coMockEitherRight(
                    { usecase.getSuggestionLocation(searchSuggestion) },
                    Location(lat, lon)
                )
                runTest { viewModel.getLocationFromSearchSuggestion(searchSuggestion) }
                then("LiveData onMoveToLocation posts the location") {
                    viewModel.onMoveToLocation().value shouldBe Location(lat, lon)
                }
            }
        }
    }

    context("Get the address of a Point") {
        given("a Point") {
            When("it's null") {
                then("return, LiveData onReverseGeocodedAddress should keep its initial null value") {
                    viewModel.getAddressFromPoint(null)
                    viewModel.onReverseGeocodedAddress().value shouldBe null
                }
            }
            When("it's not null") {
                and("the usecase returns a success containing the address") {
                    coMockEitherRight({ usecase.getAddressFromPoint(point) }, address)
                    runTest { viewModel.getAddressFromPoint(point) }
                    then("LiveData onReverseGeocodedAddress should post the value of the address") {
                        viewModel.onReverseGeocodedAddress().value shouldBe address
                    }
                }
                and("the usecase returns a failure") {
                    coMockEitherLeft({ usecase.getAddressFromPoint(point) }, failure)
                    runTest { viewModel.getAddressFromPoint(point) }
                    then("LiveData onReverseGeocodedAddress should post its value null") {
                        viewModel.onReverseGeocodedAddress().value shouldBe null
                    }
                }
            }
        }
    }

    afterSpec {
        Dispatchers.resetMain()
        stopKoin()
    }
})
