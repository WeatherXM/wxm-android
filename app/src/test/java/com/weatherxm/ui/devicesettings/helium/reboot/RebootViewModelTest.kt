package com.weatherxm.ui.devicesettings.helium.reboot

import android.bluetooth.BluetoothDevice
import com.weatherxm.R
import com.weatherxm.TestConfig.dispatcher
import com.weatherxm.TestConfig.failure
import com.weatherxm.TestConfig.resources
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.BluetoothError
import com.weatherxm.ui.InstantExecutorListener
import com.weatherxm.ui.common.ScannedDevice
import com.weatherxm.ui.common.Status
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.devicesettings.RebootState
import com.weatherxm.ui.devicesettings.RebootStatus
import com.weatherxm.usecases.BluetoothConnectionUseCase
import com.weatherxm.usecases.BluetoothScannerUseCase
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest

class RebootViewModelTest : BehaviorSpec({
    val connectionUseCase = mockk<BluetoothConnectionUseCase>()
    val scanUseCase = mockk<BluetoothScannerUseCase>()
    val analytics = mockk<AnalyticsWrapper>()
    lateinit var viewModel: RebootViewModel


    val bondFlow = MutableSharedFlow<Int>(
        replay = 1, onBufferOverflow = BufferOverflow.DROP_LATEST
    )
    val scanFlow = MutableSharedFlow<ScannedDevice>(
        replay = 1, onBufferOverflow = BufferOverflow.DROP_LATEST
    )
    val deviceNotFoundFailure = BluetoothError.DeviceNotFound
    val stationNotInRange = "Station not in range"
    val macAddress = "00:00:00"
    val pairedDevice = mockk<BluetoothDevice>()
    val device = mockk<UIDevice>()
    val scannedDevice = ScannedDevice(macAddress, macAddress)

    listener(InstantExecutorListener())

    beforeSpec {
        justRun { analytics.trackEventFailure(any()) }
        every { connectionUseCase.registerOnBondStatus() } returns bondFlow
        coEvery { scanUseCase.scan() } returns scanFlow
        coJustRun { connectionUseCase.reboot() }
        every { device.getLastCharsOfLabel() } returns macAddress
        every { connectionUseCase.getPairedDevices() } returns listOf(pairedDevice)
        every { pairedDevice.address } returns macAddress

        every {
            resources.getString(R.string.station_not_in_range_subtitle)
        } returns stationNotInRange

        viewModel = RebootViewModel(
            device,
            connectionUseCase,
            scanUseCase,
            resources,
            analytics,
            dispatcher
        )
    }

    context("Get device") {
        given("the device in the view model") {
            then("get it") {
                viewModel.device shouldBe device
            }
        }
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

    context("Flow when we want to start the connection process") {
        given("the device that just got scanned") {
            and("try to set the peripheral") {
                When("it's a failure") {
                    coMockEitherLeft(
                        { connectionUseCase.setPeripheral(scannedDevice.address) },
                        failure
                    )
                    scanFlow.tryEmit(scannedDevice)
                    runTest { viewModel.startConnectionProcess() }
                    then("analytics should track the event's failure") {
                        verify(exactly = 2) { analytics.trackEventFailure(any()) }
                    }
                    then("onStatus should post an error with the respective RebootState") {
                        viewModel.onStatus().value?.status shouldBe Status.ERROR
                        viewModel.onStatus().value?.data shouldBe RebootState(
                            RebootStatus.CONNECT_TO_STATION
                        )
                    }
                }
                When("it's a success") {
                    coMockEitherRight(
                        { connectionUseCase.setPeripheral(scannedDevice.address) },
                        Unit
                    )
                    and("try to connect to the peripheral") {
                        When("it's a failure") {
                            coMockEitherLeft(
                                { connectionUseCase.connectToPeripheral() },
                                failure
                            )
                            runTest { viewModel.startConnectionProcess() }
                            then("analytics should track the event's failure") {
                                verify(exactly = 3) { analytics.trackEventFailure(any()) }
                            }
                            then("onStatus should post an error with the respective RebootState") {
                                viewModel.onStatus().value?.status shouldBe Status.ERROR
                                viewModel.onStatus().value?.data shouldBe RebootState(
                                    RebootStatus.CONNECT_TO_STATION
                                )
                            }
                        }
                        When("it's a success") {
                            coMockEitherRight(
                                { connectionUseCase.connectToPeripheral() },
                                Unit
                            )
                            runTest { viewModel.startConnectionProcess() }
                            then("reboot in usecase should be called again") {
                                coVerify(exactly = 3) { connectionUseCase.reboot() }
                            }
                        }
                    }
                }
                then("Get the scanned device") {
                    viewModel.scannedDevice() shouldBe scannedDevice
                }
            }
        }
    }
})
