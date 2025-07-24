package com.weatherxm.ui.deeplinkrouter

import android.content.Intent
import android.net.Uri
import android.os.Build
import com.weatherxm.R
import com.weatherxm.TestConfig.DEVICE_NOT_FOUND_MSG
import com.weatherxm.TestConfig.REACH_OUT_MSG
import com.weatherxm.TestConfig.dispatcher
import com.weatherxm.TestConfig.failure
import com.weatherxm.TestConfig.resources
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.data.models.ApiError
import com.weatherxm.data.models.DataError
import com.weatherxm.data.models.RemoteMessageType
import com.weatherxm.data.models.WXMRemoteMessage
import com.weatherxm.data.repository.ExplorerRepositoryImpl.Companion.EXCLUDE_PLACES
import com.weatherxm.ui.InstantExecutorListener
import com.weatherxm.ui.common.Contracts.ARG_REMOTE_MESSAGE
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.deeplinkrouter.DeepLinkRouterViewModel.Companion.CELLS_PATH_SEGMENT
import com.weatherxm.ui.deeplinkrouter.DeepLinkRouterViewModel.Companion.STATIONS_PATH_SEGMENT
import com.weatherxm.ui.home.explorer.SearchResult
import com.weatherxm.ui.home.explorer.UICell
import com.weatherxm.usecases.DeviceListUseCase
import com.weatherxm.usecases.ExplorerUseCase
import com.weatherxm.util.AndroidBuildInfo
import com.weatherxm.util.Resources
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.core.spec.style.scopes.BehaviorSpecWhenContainerScope
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

@Suppress("DEPRECATION")
class DeepLinkRouterViewModelTest : BehaviorSpec({
    val explorerUseCase = mockk<ExplorerUseCase>()
    val devicesUseCase = mockk<DeviceListUseCase>()
    val viewModel = DeepLinkRouterViewModel(explorerUseCase, devicesUseCase, resources, dispatcher)

    val uri = mockk<Uri>()
    val intent = mockk<Intent>()
    val couldNotParseUrl = "Could not parse URL"
    val moreThanOneResults = "More than one results"
    val shareUrlNoResults = "Share URL no results"
    val errorCellNotFound = "Error cell not found"
    val normalizedStationName = "my-weather-station"
    val stationName = normalizedStationName.replace("-", " ")
    val stationUrl = "stations/$normalizedStationName"
    val cellIndex = "871eda742ffffff"
    val cellUrl = "cells/$cellIndex"
    val deviceId = "deviceId"
    val uiCell = mockk<UICell>()
    val searchResult =
        SearchResult(stationName, null, stationCellIndex = cellIndex, stationId = deviceId)

    val devices = listOf(
        UIDevice(
            deviceId,
            "",
            "",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        )
    )
    val stationRemoteMessage = WXMRemoteMessage(RemoteMessageType.STATION, deviceId = deviceId)
    val announcementRemoteMessage = WXMRemoteMessage(RemoteMessageType.ANNOUNCEMENT, url = "url")
    val emptyAnnouncementRemoteMessage = WXMRemoteMessage(RemoteMessageType.ANNOUNCEMENT)

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
        every { AndroidBuildInfo.sdkInt } returns Build.VERSION_CODES.TIRAMISU - 1
        every { intent.extras } returns mockk()
        every { intent.data } returns uri
        every { uri.path } returns ""
        every { resources.getString(R.string.could_not_parse_url) } returns couldNotParseUrl
        every { resources.getString(R.string.more_than_one_results) } returns moreThanOneResults
        every { resources.getString(R.string.share_url_no_results) } returns shareUrlNoResults
        every { resources.getString(R.string.error_cell_not_found) } returns errorCellNotFound
    }

    suspend fun BehaviorSpecWhenContainerScope.testStationNotification() {
        every {
            intent.getParcelableExtra<WXMRemoteMessage>(ARG_REMOTE_MESSAGE)
        } returns stationRemoteMessage
        When("fetching user devices fails") {
            coMockEitherLeft({ devicesUseCase.getUserDevices() }, failure)
            viewModel.parseIntent(intent)
            then("LiveData onError should post the respective error") {
                viewModel.onError().value shouldBe DEVICE_NOT_FOUND_MSG
            }
        }
        When("there are no user devices") {
            coMockEitherRight({ devicesUseCase.getUserDevices() }, emptyList<UIDevice>())
            viewModel.parseIntent(intent)
            then("LiveData onError should post the respective error") {
                viewModel.onError().value shouldBe DEVICE_NOT_FOUND_MSG
            }
        }
        When("there are user devices containing the deviceId") {
            coMockEitherRight({ devicesUseCase.getUserDevices() }, devices)
            viewModel.parseIntent(intent)
            then("LiveData onStation should post the respective Pair") {
                viewModel.onDevice().value shouldBe Pair(devices[0], false)
            }
        }
    }

    suspend fun BehaviorSpecWhenContainerScope.testAnnouncementNotification() {
        When("the url is not empty") {
            every {
                intent.getParcelableExtra<WXMRemoteMessage>(ARG_REMOTE_MESSAGE)
            } returns announcementRemoteMessage
            viewModel.parseIntent(intent)
            then("LiveData onAnnouncement should post the respective url") {
                viewModel.onAnnouncement().value shouldBe announcementRemoteMessage.url
            }
        }
        When("the url is empty") {
            every {
                intent.getParcelableExtra<WXMRemoteMessage>(ARG_REMOTE_MESSAGE)
            } returns emptyAnnouncementRemoteMessage
            viewModel.parseIntent(intent)
            then("LiveData onError should post the respective error") {
                viewModel.onError().value shouldBe couldNotParseUrl
            }
        }
    }

    suspend fun BehaviorSpecWhenContainerScope.testStationDeepLink() {
        When("network search fails") {
            coMockEitherLeft(
                { explorerUseCase.networkSearch(stationName, true, EXCLUDE_PLACES) },
                ApiError.DeviceNotFound("")
            )
            runTest { viewModel.parseIntent(intent) }
            then("LiveData onError should post the respective error") {
                viewModel.onError().value shouldBe DEVICE_NOT_FOUND_MSG
            }
        }
        When("network search succeeds") {
            and("results are empty") {
                coMockEitherRight(
                    { explorerUseCase.networkSearch(stationName, true, EXCLUDE_PLACES) },
                    emptyList<SearchResult>()
                )
                runTest { viewModel.parseIntent(intent) }
                then("LiveData onError should post the respective error") {
                    viewModel.onError().value shouldBe shareUrlNoResults
                }
            }
            and("results contain more than 1 element") {
                coMockEitherRight(
                    { explorerUseCase.networkSearch(stationName, true, EXCLUDE_PLACES) },
                    listOf<SearchResult>(mockk(), mockk())
                )
                runTest { viewModel.parseIntent(intent) }
                then("LiveData onError should post the respective error") {
                    viewModel.onError().value shouldBe moreThanOneResults
                }
            }
            and("results contain exactly 1 element") {
                coMockEitherRight(
                    { explorerUseCase.networkSearch(stationName, true, EXCLUDE_PLACES) },
                    listOf(searchResult)
                )
                and("get user devices fails") {
                    coMockEitherLeft({ devicesUseCase.getUserDevices() }, failure)
                    and("get cell device fails") {
                        coMockEitherLeft(
                            { explorerUseCase.getCellDevice(cellIndex, deviceId) },
                            failure
                        )
                        runTest { viewModel.parseIntent(intent) }
                        then("LiveData onError should post the respective error") {
                            viewModel.onError().value shouldBe REACH_OUT_MSG
                        }
                    }
                    and("get cell device succeeds") {
                        coMockEitherRight(
                            { explorerUseCase.getCellDevice(cellIndex, deviceId) },
                            devices[0]
                        )
                        runTest { viewModel.parseIntent(intent) }
                        then("LiveData onDevice should post the respective Pair") {
                            viewModel.onDevice().value shouldBe Pair(devices[0], true)
                        }
                    }
                }
                and("get user devices succeeds and it's an owned station") {
                    coMockEitherRight({ devicesUseCase.getUserDevices() }, devices)
                    runTest { viewModel.parseIntent(intent) }
                    then("LiveData onDevice should post the respective Pair") {
                        viewModel.onDevice().value shouldBe Pair(devices[0], true)
                    }
                }
            }
        }
    }

    suspend fun BehaviorSpecWhenContainerScope.testCellDeepLink() {
        When("fetching cell info fails") {
            coMockEitherLeft({ explorerUseCase.getCellInfo(cellIndex) }, DataError.CellNotFound)
            runTest { viewModel.parseIntent(intent) }
            then("LiveData onError should post the respective error") {
                viewModel.onError().value shouldBe errorCellNotFound
            }
        }
        When("fetching cell info succeeds") {
            coMockEitherRight({ explorerUseCase.getCellInfo(cellIndex) }, uiCell)
            runTest { viewModel.parseIntent(intent) }
            then("LiveData onCell should post the respective cell") {
                viewModel.onCell().value shouldBe uiCell
            }
        }
    }

    context("Parse an intent") {
        When("intent is a remote message (notification)") {
            and("it is a station notification") {
                testStationNotification()
            }
            and("it is an announcement notification") {
                testAnnouncementNotification()
            }
        }
        When("intent is not a remote message (it's a URL that needs parsing)") {
            every { intent.getParcelableExtra<WXMRemoteMessage>(ARG_REMOTE_MESSAGE) } returns null
            and("the URI does not have any path segments") {
                every { uri.pathSegments } returns null
                viewModel.parseIntent(intent)
                then("LiveData onError should post the respective error") {
                    viewModel.onError().value shouldBe couldNotParseUrl
                }
            }
            and("the URI has the required path segments") {
                When("the path starts with $STATIONS_PATH_SEGMENT") {
                    every { uri.pathSegments } returns stationUrl.split("/")
                    testStationDeepLink()
                }
                When("the path starts with $CELLS_PATH_SEGMENT") {
                    every { uri.pathSegments } returns cellUrl.split("/")
                    testCellDeepLink()
                }
            }
        }
    }

    afterSpec {
        stopKoin()
    }
})
