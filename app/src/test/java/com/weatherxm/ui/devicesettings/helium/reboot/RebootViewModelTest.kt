package com.weatherxm.ui.devicesettings.helium.reboot

import com.weatherxm.R
import com.weatherxm.TestConfig.failure
import com.weatherxm.TestConfig.resources
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.BluetoothError
import com.weatherxm.ui.InstantExecutorListener
import com.weatherxm.ui.common.Status
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.devicesettings.RebootState
import com.weatherxm.ui.devicesettings.RebootStatus
import com.weatherxm.usecases.BluetoothConnectionUseCase
import com.weatherxm.usecases.BluetoothScannerUseCase
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coJustRun
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class RebootViewModelTest : BehaviorSpec({
    val connectionUseCase = mockk<BluetoothConnectionUseCase>()
    val scanUseCase = mockk<BluetoothScannerUseCase>()
    val analytics = mockk<AnalyticsWrapper>()
    lateinit var viewModel: RebootViewModel

    val device = mockk<UIDevice>()

    val bondFlow = MutableSharedFlow<Int>(
        replay = 1, onBufferOverflow = BufferOverflow.DROP_LATEST
    )
    val deviceNotFoundFailure = BluetoothError.DeviceNotFound
    val stationNotInRange = "Station not in range"

    listener(InstantExecutorListener())
    Dispatchers.setMain(StandardTestDispatcher())

    beforeSpec {
        justRun { analytics.trackEventFailure(any()) }
        every { connectionUseCase.registerOnBondStatus() } returns bondFlow
        coJustRun { connectionUseCase.reboot() }
        every { device.getLastCharsOfLabel() } returns "00:00:00"

        every {
            resources.getString(R.string.station_not_in_range_subtitle)
        } returns stationNotInRange

        viewModel = RebootViewModel(
            device,
            connectionUseCase,
            scanUseCase,
            resources,
            analytics
        )
    }

    context("Flow when onNotPaired gets triggered") {
        given("the trigger") {
            viewModel.onNotPaired()
            then("onStatus should post an error with the respective RebootState") {
                viewModel.onStatus().value?.status shouldBe Status.ERROR
                viewModel.onStatus().value?.data shouldBe RebootState(RebootStatus.PAIR_STATION)
            }
        }
    }

    context("Flow when onScanFailure gets triggered") {
        given("a failure") {
            When("it's a BluetoothError.DeviceNotFound") {
                viewModel.onScanFailure(deviceNotFoundFailure)
                then("onStatus should post an error with the respective RebootState") {
                    viewModel.onStatus().value?.status shouldBe Status.ERROR
                    viewModel.onStatus().value?.message shouldBe stationNotInRange
                    viewModel.onStatus().value?.data shouldBe RebootState(
                        RebootStatus.SCAN_FOR_STATION, BluetoothError.DeviceNotFound
                    )
                }
            }
            When("it's any other failure") {
                viewModel.onScanFailure(failure)
                then("LiveData onStatus posts a generic error with the respective RebootState") {
                    viewModel.onStatus().value?.status shouldBe Status.ERROR
                    viewModel.onStatus().value?.data shouldBe RebootState(
                        RebootStatus.SCAN_FOR_STATION
                    )
                }
            }
        }
    }

    context("Flow when onConnectionFailure gets triggered") {
        given("the trigger") {
            viewModel.onConnectionFailure(failure)
            then("onStatus should post an error with the respective RebootState") {
                viewModel.onStatus().value?.status shouldBe Status.ERROR
                viewModel.onStatus().value?.data shouldBe RebootState(
                    RebootStatus.CONNECT_TO_STATION
                )
            }
        }
    }

    context("Start the reboot process when device is connected") {
        given("a usecase returning the result of the reboot") {
            When("it's a failure") {
                coMockEitherLeft({ connectionUseCase.reboot() }, failure)
                runTest { viewModel.onConnected() }
                then("analytics should track the event's failure") {
                    verify(exactly = 1) { analytics.trackEventFailure(any()) }
                }
                then("onStatus should post an error with the respective RebootState") {
                    viewModel.onStatus().value?.status shouldBe Status.ERROR
                    viewModel.onStatus().value?.data shouldBe RebootState(RebootStatus.REBOOTING)
                }
            }
            When("it's a success") {
                coMockEitherRight({ connectionUseCase.reboot() }, Unit)
                runTest { viewModel.onConnected() }
                then("onStatus should post a success with the respective RebootState") {
                    viewModel.onStatus().value?.status shouldBe Status.SUCCESS
                    viewModel.onStatus().value?.data shouldBe RebootState(RebootStatus.REBOOTING)
                }
            }
        }
    }

    /**
     * TODO: startConnectionProcess is missing due to the issue on CountDownTimer,
     * we need to move this functionality in an util class or sth so we can mock it in the tests.
     */
//    context("Flow when we want to start the connection process") {
//        given("the trigger") {
//            viewModel.startConnectionProcess()
//        }
//    }

    afterSpec {
        Dispatchers.resetMain()
    }
})
