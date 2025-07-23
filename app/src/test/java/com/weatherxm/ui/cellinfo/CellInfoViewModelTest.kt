package com.weatherxm.ui.cellinfo

import com.weatherxm.R
import com.weatherxm.TestConfig.DEVICE_NOT_FOUND_MSG
import com.weatherxm.TestConfig.REACH_OUT_MSG
import com.weatherxm.TestConfig.dispatcher
import com.weatherxm.TestConfig.failure
import com.weatherxm.TestConfig.resources
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isError
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.TestUtils.testHandleFailureViewModel
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.ApiError
import com.weatherxm.data.models.Location
import com.weatherxm.ui.InstantExecutorListener
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.home.explorer.UICell
import com.weatherxm.usecases.AuthUseCase
import com.weatherxm.usecases.ExplorerUseCase
import com.weatherxm.usecases.FollowUseCase
import com.weatherxm.util.Resources
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.core.spec.style.scopes.BehaviorSpecWhenContainerScope
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class CellInfoViewModelTest : BehaviorSpec({
    val explorerUseCase = mockk<ExplorerUseCase>()
    val followUseCase = mockk<FollowUseCase>()
    val authUseCase = mockk<AuthUseCase>()
    val analytics = mockk<AnalyticsWrapper>()
    val cell = UICell("", Location(0.0, 0.0))
    lateinit var viewModel: CellInfoViewModel

    val cellDevicesNoData = "No Data for this cell"
    val maxFollowedMsg = "Max followed error"
    val address = "address"
    val deviceId = "deviceId"
    val devices = listOf(
        UIDevice.empty(),
        UIDevice(
            "",
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
            address = address,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        )
    )
    val deviceNotFoundFailure = ApiError.DeviceNotFound("")
    val maxFollowedFailure = ApiError.MaxFollowed("")
    val unauthorizedFailure = ApiError.GenericError.JWTError.UnauthorizedError("", "unauthorized")

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
        justRun { analytics.trackEventUserAction(any(), any()) }
        every { authUseCase.isLoggedIn() } returns true
        every { resources.getString(R.string.error_cell_devices_no_data) } returns cellDevicesNoData
        every { resources.getString(R.string.error_max_followed) } returns maxFollowedMsg

        viewModel = CellInfoViewModel(
            cell,
            resources,
            explorerUseCase,
            followUseCase,
            authUseCase,
            analytics,
            dispatcher
        )
    }

    suspend fun BehaviorSpecWhenContainerScope.testFollowFailure(
        verifyNumberOfFailureEvents: Int,
        errorMsg: String?
    ) {
        testHandleFailureViewModel(
            { viewModel.followStation(deviceId) },
            analytics,
            viewModel.onFollowStatus(),
            verifyNumberOfFailureEvents,
            errorMsg ?: String.empty()
        )
    }

    suspend fun BehaviorSpecWhenContainerScope.testUnfollowFailure(
        verifyNumberOfFailureEvents: Int,
        errorMsg: String?
    ) {
        testHandleFailureViewModel(
            { viewModel.unFollowStation(deviceId) },
            analytics,
            viewModel.onFollowStatus(),
            verifyNumberOfFailureEvents,
            errorMsg ?: String.empty()
        )
    }

    context("Get if the user is logged in or not") {
        given("a usecase providing if the user is logged in or not") {
            then("return the result") {
                viewModel.isLoggedIn() shouldBe true
            }
        }
    }

    context("Get cell devices") {
        given("a usecase providing the cell devices") {
            When("index is empty") {
                then("LiveData at onCellDevices posts an error") {
                    runTest { viewModel.fetchDevices() }
                    viewModel.onCellDevices().isError(cellDevicesNoData)
                }
            }
            When("the index is NOT empty") {
                cell.index = "index"
                and("usecase returns a failure") {
                    coMockEitherLeft({ explorerUseCase.getCellDevices(cell) }, failure)
                    testHandleFailureViewModel(
                        { viewModel.fetchDevices() },
                        analytics,
                        viewModel.onCellDevices(),
                        1,
                        cellDevicesNoData
                    )
                }
                and("usecase returns a success") {
                    coMockEitherRight({ explorerUseCase.getCellDevices(cell) }, devices)
                    runTest { viewModel.fetchDevices() }
                    then("LiveData at onCellDevices posts the devices") {
                        viewModel.onCellDevices().isSuccess(devices)
                    }
                    then("LiveData at address posts the first address found") {
                        viewModel.address().value shouldBe address
                    }
                }
            }
        }
    }

    context("Follow a station") {
        given("a usecase providing the response of the follow request") {
            When("it is a failure") {
                and("It's a DeviceNotFound failure") {
                    coMockEitherLeft(
                        { followUseCase.followStation(deviceId) },
                        deviceNotFoundFailure
                    )
                    testFollowFailure(2, DEVICE_NOT_FOUND_MSG)
                }
                and("It's a MaxFollowed failure") {
                    coMockEitherLeft({ followUseCase.followStation(deviceId) }, maxFollowedFailure)
                    testFollowFailure(3, maxFollowedMsg)
                }
                and("It's an UnauthorizedFailure failure") {
                    coMockEitherLeft({ followUseCase.followStation(deviceId) }, unauthorizedFailure)
                    testFollowFailure(4, unauthorizedFailure.message)
                }
                and("it's any other failure") {
                    coMockEitherLeft({ followUseCase.followStation(deviceId) }, failure)
                    testFollowFailure(5, REACH_OUT_MSG)
                }
            }
            When("it is a success") {
                coMockEitherRight({ followUseCase.followStation(deviceId) }, Unit)
                /**
                 * Set an empty list of devices as the response of getCellDevices
                 */
                coMockEitherRight(
                    { explorerUseCase.getCellDevices(cell) },
                    emptyList<UIDevice>()
                )

                runTest { viewModel.followStation(deviceId) }

                then("LiveData at onFollowStatus posts a success") {
                    viewModel.onFollowStatus().isSuccess(Unit)
                }
                then("fetch devices again") {
                    viewModel.onCellDevices().isSuccess(emptyList())
                }
            }
        }
    }

    context("Unfollow a station") {
        given("a usecase providing the response of the unfollow request") {
            When("it is a failure") {
                and("It's a DeviceNotFound failure") {
                    coMockEitherLeft(
                        { followUseCase.unfollowStation(deviceId) },
                        deviceNotFoundFailure
                    )
                    testUnfollowFailure(6, DEVICE_NOT_FOUND_MSG)
                }
                and("It's a MaxFollowed failure") {
                    coMockEitherLeft(
                        { followUseCase.unfollowStation(deviceId) },
                        maxFollowedFailure
                    )
                    testUnfollowFailure(7, maxFollowedMsg)
                }
                and("It's an UnauthorizedFailure failure") {
                    coMockEitherLeft(
                        { followUseCase.unfollowStation(deviceId) },
                        unauthorizedFailure
                    )
                    testUnfollowFailure(8, unauthorizedFailure.message)
                }
                and("it's any other failure") {
                    coMockEitherLeft({ followUseCase.unfollowStation(deviceId) }, failure)
                    testUnfollowFailure(9, REACH_OUT_MSG)
                }
            }
            When("it is a success") {
                coMockEitherRight({ followUseCase.unfollowStation(deviceId) }, Unit)
                /**
                 * Set again devices as the response of getCellDevices
                 */
                coMockEitherRight({ explorerUseCase.getCellDevices(cell) }, devices)

                runTest { viewModel.unFollowStation(deviceId) }

                then("LiveData at onFollowStatus posts a success") {
                    viewModel.onFollowStatus().isSuccess(Unit)
                }
                then("fetch devices again") {
                    viewModel.onCellDevices().isSuccess(devices)
                }
            }
        }
    }

    afterSpec {
        stopKoin()
    }
})
