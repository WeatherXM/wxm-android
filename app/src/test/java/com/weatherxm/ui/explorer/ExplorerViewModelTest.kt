package com.weatherxm.ui.explorer

import com.mapbox.geojson.Point
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.extension.style.layers.generated.HeatmapLayer
import com.mapbox.maps.extension.style.layers.generated.heatmapLayer
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotationOptions
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
import com.weatherxm.data.models.PublicHex
import com.weatherxm.ui.InstantExecutorListener
import com.weatherxm.ui.components.BaseMapFragment.Companion.DEFAULT_ZOOM_LEVEL
import com.weatherxm.ui.components.BaseMapFragment.Companion.ZOOMED_IN_ZOOM_LEVEL
import com.weatherxm.ui.explorer.ExplorerViewModel.Companion.HEATMAP_LAYER_ID
import com.weatherxm.ui.explorer.ExplorerViewModel.Companion.HEATMAP_LAYER_SOURCE
import com.weatherxm.ui.explorer.ExplorerViewModel.Companion.HEATMAP_SOURCE_ID
import com.weatherxm.ui.explorer.ExplorerViewModel.Companion.HEATMAP_WEIGHT_KEY
import com.weatherxm.usecases.ExplorerUseCase
import com.weatherxm.util.LocationHelper
import com.weatherxm.util.MapboxUtils
import com.weatherxm.util.MapboxUtils.toPolygonAnnotationOptions
import com.weatherxm.util.Resources
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class ExplorerViewModelTest : BehaviorSpec({
    val usecase = mockk<ExplorerUseCase>()
    val locationHelper = mockk<LocationHelper>()
    val analytics = mockk<AnalyticsWrapper>()
    lateinit var viewModel: ExplorerViewModel

    val startingLocation = Location(1.0, 1.0)
    val location = Location(0.0, 0.0)
    val startingNavigationLocation = NavigationLocation(DEFAULT_ZOOM_LEVEL, startingLocation)
    val navigationLocation = NavigationLocation(ZOOMED_IN_ZOOM_LEVEL, location)
    val cameraZoom = 10.0
    val cameraCenter = mockk<Point>()
    val explorerCamera = ExplorerCamera(cameraZoom, cameraCenter)
    val locationSlot = slot<(location: Location?) -> Unit>()

    /**
     * The below line produces the following error in logs:
     * java.lang.RuntimeException: Method run in android.os.HandlerThread not mocked.
     *
     * Will need to investigate further what this is.
     */
    val geoJsonSource = mockk<GeoJsonSource>()

    val publicHex = PublicHex("cellIndex", 1, location, listOf())
    val publicHex2 = PublicHex("cellIndex2", 1, location, listOf())
    val newPolygonAnnotationOptions = listOf(mockk<PolygonAnnotationOptions>())
    val explorerData = ExplorerData(geoJsonSource, listOf(publicHex), listOf())
    val newExplorerData =
        ExplorerData(geoJsonSource, listOf(publicHex2), newPolygonAnnotationOptions)
    val fullExplorerData = ExplorerData(geoJsonSource, listOf(publicHex, publicHex2), listOf())
    val heatmapLayer: HeatmapLayer by lazy {
        heatmapLayer(
            HEATMAP_LAYER_ID,
            HEATMAP_SOURCE_ID
        ) {
            maxZoom(10.0) // Hide layer at zoom level 10
            sourceLayer(HEATMAP_LAYER_SOURCE)
            // Begin color ramp at 0-stop with a 0-transparency color
            // to create a blur-like effect.
            heatmapColor(
                interpolate {
                    linear()
                    heatmapDensity()
                    stop {
                        literal(0)
                        rgba(33.0, 102.0, 172.0, 0.0)
                    }
                    stop {
                        literal(0.2)
                        rgb(103.0, 169.0, 207.0)
                    }
                    stop {
                        literal(0.4)
                        rgb(162.0, 187.0, 201.0)
                    }
                    stop {
                        literal(0.6)
                        rgb(149.0, 153.0, 189.0)
                    }
                    stop {
                        literal(0.8)
                        rgb(103.0, 118.0, 247.0)
                    }
                    stop {
                        literal(1)
                        rgb(0.0, 255.0, 206.0)
                    }
                }
            )
            // Increase the heatmap weight based on the device count
            heatmapWeight(
                interpolate {
                    linear()
                    get { literal(HEATMAP_WEIGHT_KEY) }
                    stop {
                        literal(0)
                        literal(0) // 0 weight at 0 cell device count
                    }
                    stop {
                        literal(100)
                        literal(100) // 100 weight at 100 cell device count
                    }
                }
            )
            // Adjust the heatmap radius by zoom level
            heatmapRadius(
                interpolate {
                    linear()
                    zoom()
                    stop {
                        literal(0.0)
                        literal(2) // 2 radius at zoom level 0.0
                    }
                    stop {
                        literal(9.0)
                        literal(20) // 20 radius at zoom level 9.0
                    }
                }
            )
            // Adjust heatmap opacity based on zoom level, until it totally disappears
            heatmapOpacity(
                interpolate {
                    exponential {
                        literal(0.5) // Exponential interpolation base
                    }
                    zoom()
                    stop {
                        literal(0.0)
                        literal(1.0) // 1.0 opacity at zoom level 0.0
                    }
                    stop {
                        literal(8.0)
                        literal(0.9) // 0.9 opacity at zoom level 8.0
                    }
                    stop {
                        literal(9.0)
                        literal(0.5) // 0.5 opacity at zoom level 9.0
                    }
                    stop {
                        literal(9.5)
                        literal(0.1) // 0.1 opacity at zoom level 9.5
                    }
                    stop {
                        literal(10.0)
                        literal(0.0)  // 0.0 opacity at zoom level 10.0
                    }
                }
            )
        }
    }

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
        every { locationHelper.hasLocationPermissions() } returns false
        coEvery { usecase.getUserCountryLocation() } returns startingLocation
        mockkObject(MapboxUtils)
        every { explorerData.publicHexes.toPolygonAnnotationOptions() } returns emptyList()
        every { fullExplorerData.publicHexes.toPolygonAnnotationOptions() } returns emptyList()
        every {
            newExplorerData.publicHexes.toPolygonAnnotationOptions()
        } returns newPolygonAnnotationOptions

        viewModel = ExplorerViewModel(usecase, analytics, locationHelper, dispatcher)
    }

    context("Ensure that the heatmap is defined correctly") {
        given("a heatmap") {
            then("ensure that its options are correct") {
                viewModel.heatmapLayer.toString() shouldBe heatmapLayer.toString()
            }
        }
    }

    context("Navigation to the user location") {
        given("a location and if this got triggered by the user location or not") {
            When("it's a user location automatically fetched on init on start-up") {
                then("LiveData onNavigateToLocation should be called with DEFAULT_ZOOM_LEVEL") {
                    viewModel.onNavigateToLocation().value shouldBe startingNavigationLocation
                }
            }
            When("it's not a user location but a custom one") {
                viewModel.navigateToLocation(location)
                then("LiveData onNavigateToLocation posts the correct NavigationLocation") {
                    viewModel.onNavigateToLocation().value shouldBe navigationLocation
                }
            }
        }
    }

    context("GET / SET the variable showing if we are in a state in the logged-in explorer") {
        When("GET the state if we are in the logged-in explorer or not") {
            then("return the default false value") {
                viewModel.isExplorerAfterLoggedIn() shouldBe false
            }
        }
        When("SET a new state") {
            viewModel.setExplorerAfterLoggedIn(true)
            then("GET it to ensure it has been set") {
                viewModel.isExplorerAfterLoggedIn() shouldBe true
            }
        }
    }

    context("Flow to run when the GPS button of user's location has been clicked") {
        given("the function's call") {
            viewModel.onMyLocation()
            then("LiveData `onMyLocation` should post the value true") {
                viewModel.onMyLocationClicked().value shouldBe true
            }
        }
    }

    context("Fetch Explorer Data") {
        given("a usecase returning the ExplorerData") {
            When("it's a failure") {
                coMockEitherLeft({ usecase.getCells() }, failure)
                testHandleFailureViewModel(
                    { viewModel.fetch() },
                    analytics,
                    viewModel.onStatus(),
                    1,
                    REACH_OUT_MSG
                )
            }
            When("it's a success") {
                coMockEitherRight({ usecase.getCells() }, explorerData)
                and("the map is currently empty") {
                    runTest { viewModel.fetch() }
                    then("LiveData onExplorerData should post the updated ExplorerData") {
                        viewModel.onExplorerData().value shouldBe explorerData
                    }
                    then("LiveData onStatus should post the success value Unit") {
                        viewModel.onStatus().isSuccess(Unit)
                    }
                }
                and("the map already has some data to show") {
                    and("there are no new data fetched") {
                        coMockEitherRight({ usecase.getCells() }, explorerData)
                        runTest { viewModel.fetch() }
                        then("LiveData onNewPolygons should post the value of an empty list") {
                            viewModel.onNewPolygons().value shouldBe emptyList()
                        }
                        then("LiveData onStatus should post the success value Unit") {
                            viewModel.onStatus().isSuccess(Unit)
                        }
                    }
                    and("there some new data fetched") {
                        coMockEitherRight({ usecase.getCells() }, fullExplorerData)
                        runTest { viewModel.fetch() }
                        then("LiveData onNewPolygons should post only the new Polygons") {
                            viewModel.onNewPolygons().value shouldBe newPolygonAnnotationOptions
                        }
                        then("LiveData onStatus should post the success value Unit") {
                            viewModel.onStatus().isSuccess(Unit)
                        }
                    }
                }
            }
        }
    }

    context("Flow to run when the map has been clicked and we need to show/hide overlay views") {
        given("if the overlay views are currently visible") {
            When("they are visible") {
                viewModel.onMapClick()
                then("LiveData `showMapOverlayViews` should post the value false") {
                    viewModel.showMapOverlayViews().value shouldBe false
                }
            }
            When("they are hidden") {
                viewModel.onMapClick()
                then("LiveData `showMapOverlayViews` should post the value true") {
                    viewModel.showMapOverlayViews().value shouldBe true
                }
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

    context("GET / SET the current ExplorerCamera of the map") {
        When("GET the camera") {
            then("return the default ExplorerCamera = null") {
                viewModel.getCurrentCamera() shouldBe null
            }
        }
        When("SET a new ExplorerCamera") {
            viewModel.setCurrentCamera(cameraZoom, cameraCenter)
            then("GET it to ensure it has been set") {
                viewModel.getCurrentCamera() shouldBe explorerCamera
            }
        }
    }

    context("Get user's location") {
        given("a request to the user to provide it") {
            and("use location helper to get the user's location") {
                every {
                    locationHelper.getLocationAndThen(capture(locationSlot))
                } answers {
                    locationSlot.captured.invoke(location)
                }
                viewModel.getLocation {
                    it shouldBe location
                }
            }
        }
    }

    afterSpec {
        stopKoin()
    }
})
