package com.weatherxm.ui.deviceeditlocation

import com.mapbox.geojson.Point
import com.mapbox.search.result.SearchSuggestion
import com.weatherxm.R
import com.weatherxm.TestConfig.DEVICE_NOT_FOUND_MSG
import com.weatherxm.TestConfig.REACH_OUT_MSG
import com.weatherxm.TestConfig.failure
import com.weatherxm.TestConfig.resources
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.TestUtils.testHandleFailureViewModel
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.ApiError
import com.weatherxm.data.models.Location
import com.weatherxm.ui.InstantExecutorListener
import com.weatherxm.ui.common.UIDevice
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
class DeviceEditLocationViewModelTest : BehaviorSpec({
    val usecase = mockk<EditLocationUseCase>()
    val locationHelper = mockk<LocationHelper>()
    val analytics = mockk<AnalyticsWrapper>()
    lateinit var viewModel: DeviceEditLocationViewModel

    val deviceId = "deviceId"
    val query = "query"
    val address = "address"
    val lat = 10.0
    val lon = 10.0
    val locationSlot = slot<(location: Location?) -> Unit>()
    val searchSuggestion = mockk<SearchSuggestion>()
    val point = mockk<Point>()
    val device = UIDevice.empty()

    val invalidLocation = "invalidLocation"
    val invalidLocationFailure = ApiError.UserError.ClaimError.InvalidClaimLocation("")
    val deviceNotFoundFailure = ApiError.DeviceNotFound("")

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
        every { resources.getString(R.string.error_invalid_location) } returns invalidLocation
        justRun { analytics.trackEventFailure(any()) }

        viewModel = DeviceEditLocationViewModel(usecase, analytics, locationHelper, resources)
    }

    context("Validate a location") {
        given("a location") {
            then("validate it") {
                viewModel.validateLocation(lat, lon) shouldBe true
            }
        }
    }

    context("Get user's location") {
        given("a helper class that returns the user location") {
            and("use location helper to get the user's location") {
                every {
                    locationHelper.getLocationAndThen(capture(locationSlot))
                } answers {
                    locationSlot.captured.invoke(Location(lat, lon))
                }
                viewModel.getLocation()
                then("LiveData onMoveToLocation should post the location") {
                    viewModel.onMoveToLocation().value shouldBe Location(lat, lon)
                }
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
                    then("track event's failure in analytics") {
                        verify(exactly = 2) { analytics.trackEventFailure(any()) }
                    }
                    then("LiveData onReverseGeocodedAddress should post its value null") {
                        viewModel.onReverseGeocodedAddress().value shouldBe null
                    }
                }
            }
        }
    }

    context("set the new location") {
        given("a usecase returning the response regarding the setting of the new location") {
            When("it's a failure") {
                and("It's an InvalidClaimLocation failure") {
                    coMockEitherLeft(
                        { usecase.setLocation(deviceId, lat, lon) },
                        invalidLocationFailure
                    )
                    testHandleFailureViewModel(
                        { viewModel.setLocation(deviceId, lat, lon) },
                        analytics,
                        viewModel.onUpdatedDevice(),
                        3,
                        invalidLocation
                    )
                }
                and("It's an DeviceNotFound failure") {
                    coMockEitherLeft(
                        { usecase.setLocation(deviceId, lat, lon) },
                        deviceNotFoundFailure
                    )
                    testHandleFailureViewModel(
                        { viewModel.setLocation(deviceId, lat, lon) },
                        analytics,
                        viewModel.onUpdatedDevice(),
                        4,
                        DEVICE_NOT_FOUND_MSG
                    )
                }
                and("it's any other failure") {
                    coMockEitherLeft({ usecase.setLocation(deviceId, lat, lon) }, failure)
                    testHandleFailureViewModel(
                        { viewModel.setLocation(deviceId, lat, lon) },
                        analytics,
                        viewModel.onUpdatedDevice(),
                        5,
                        REACH_OUT_MSG
                    )
                }
            }
            When("it's a success") {
                coMockEitherRight({ usecase.setLocation(deviceId, lat, lon) }, device)
                runTest { viewModel.setLocation(deviceId, lat, lon) }
                then("LiveData onUpdatedDevice should post the updated device") {
                    viewModel.onUpdatedDevice().isSuccess(device)
                }
            }
        }
    }

    afterSpec {
        Dispatchers.resetMain()
        stopKoin()
    }
})
