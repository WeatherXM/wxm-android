package com.weatherxm.ui.home.locations

import com.weatherxm.TestConfig.REACH_OUT_MSG
import com.weatherxm.TestConfig.dispatcher
import com.weatherxm.TestConfig.failure
import com.weatherxm.TestConfig.resources
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.TestUtils.testHandleFailureViewModel
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.Location
import com.weatherxm.ui.InstantExecutorListener
import com.weatherxm.ui.common.LocationWeather
import com.weatherxm.ui.common.LocationsWeather
import com.weatherxm.usecases.LocationsUseCase
import com.weatherxm.util.Resources
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class LocationsViewModelTest : BehaviorSpec({
    val usecase = mockk<LocationsUseCase>()
    val analytics = mockk<AnalyticsWrapper>()
    lateinit var viewModel: LocationsViewModel

    val currentLocation = Location.empty()
    val location = Location(10.0, 10.0)
    val currentWeather = mockk<LocationWeather>()
    val savedWeather = mockk<LocationWeather>()
    val locationsWeatherEmpty = LocationsWeather(
        current = null,
        saved = listOf()
    )
    val locationsWeatherNoCurrent = LocationsWeather(
        current = null,
        saved = listOf(savedWeather)
    )
    val locationsWeatherWithData = LocationsWeather(
        current = currentWeather,
        saved = listOf(savedWeather)
    )

    listener(InstantExecutorListener())

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
        justRun { usecase.clearLocationForecastFromCache() }
        every { usecase.getSavedLocations() } returns listOf(location)

        viewModel = LocationsViewModel(usecase, analytics, dispatcher)
    }

    context("Fetch the LocationsWeather object") {
        given("if a user is logged in and has given current location") {
            When("user is logged in") {
                and("has not given current location") {
                    and("there are no saved locations") {
                        then("return the LocationsWeather WITHOUT the current weather") {
                            runTest { viewModel.fetch(null, true) }
                            viewModel.onLocationsWeather().isSuccess(locationsWeatherEmpty)
                        }
                    }
                    and("there are saved locations") {
                        viewModel.getSavedLocations()
                        and("the response of the saved location from the usecase is a failure") {
                            coMockEitherLeft({ usecase.getLocationWeather(location) }, failure)
                            testHandleFailureViewModel(
                                { viewModel.fetch(null, true) },
                                analytics,
                                viewModel.onLocationsWeather(),
                                1,
                                REACH_OUT_MSG
                            )
                        }
                        and("the response of the saved location from the usecase is a success") {
                            coMockEitherRight(
                                { usecase.getLocationWeather(location) },
                                savedWeather
                            )
                            then("return the LocationsWeather WITHOUT the current weather") {
                                runTest { viewModel.fetch(null, true) }
                                viewModel.onLocationsWeather().isSuccess(locationsWeatherNoCurrent)
                            }
                        }
                    }
                }
                and("has given current location") {
                    and("the response of the current location from the usecase is a failure") {
                        coMockEitherLeft({ usecase.getLocationWeather(currentLocation) }, failure)
                        testHandleFailureViewModel(
                            { viewModel.fetch(currentLocation, true) },
                            analytics,
                            viewModel.onLocationsWeather(),
                            2,
                            REACH_OUT_MSG
                        )
                    }
                    and("the response of the current location from the usecase is a success") {
                        coMockEitherRight(
                            { usecase.getLocationWeather(currentLocation) },
                            currentWeather
                        )
                        coMockEitherRight({ usecase.getLocationWeather(location) }, savedWeather)
                        then("return the LocationsWeather WITH the current weather") {
                            runTest { viewModel.fetch(currentLocation, true) }
                            viewModel.onLocationsWeather().isSuccess(locationsWeatherWithData)
                        }
                    }
                }
            }
            When("the user is NOT logged in") {
                then("return only the first one of the saved locatons") {
                    coMockEitherRight(
                        { usecase.getLocationWeather(currentLocation) },
                        currentWeather
                    )
                    coMockEitherRight({ usecase.getLocationWeather(location) }, savedWeather)
                    runTest { viewModel.fetch(currentLocation, false) }
                    viewModel.onLocationsWeather().isSuccess(locationsWeatherWithData)
                }

            }
        }
    }

    context("Clear location forecast from cache") {
        given("A usecase which gets called to trigger it") {
            then("ensure that the call took place") {
                viewModel.clearLocationForecastFromCache()
                verify(exactly = 1) { usecase.clearLocationForecastFromCache() }
            }
        }
    }

    context("Get saved locations and check if a location is saved") {
        given("A usecase which returns the saved locations") {
            then("return them") {
                viewModel.getSavedLocations() shouldBe listOf(location)
            }
            then("return if a location is included in the saved ones") {
                viewModel.isLocationSaved(location) shouldBe true
            }
        }
    }

    context("Flow to run when the fragment invokes the onSearchOpenStatus function") {
        given("the status if the search is open or not") {
            When("it's open") {
                viewModel.onSearchOpenStatus(true)
                then("LiveData `onSearchOpenStatus` should post the value true") {
                    viewModel.onSearchOpenStatus().value shouldBe true
                }
            }
            When("it's closed") {
                viewModel.onSearchOpenStatus(false)
                then("LiveData `onSearchOpenStatus` should post the value false") {
                    viewModel.onSearchOpenStatus().value shouldBe false
                }
            }
        }
    }

    context("Flow to run when the function searchBtnClicked is triggered") {
        given("the function call") {
            then("The SingleLiveEvent `onSearchClicked` should post a Unit value") {
                runTest { viewModel.searchBtnClicked() }
                viewModel.onSearchClicked().value shouldBe Unit
            }
        }
    }

    afterSpec {
        stopKoin()
    }
})
