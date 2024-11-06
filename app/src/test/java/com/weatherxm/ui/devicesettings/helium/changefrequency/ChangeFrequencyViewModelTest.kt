package com.weatherxm.ui.devicesettings.helium.changefrequency

import com.weatherxm.R
import com.weatherxm.TestConfig.failure
import com.weatherxm.TestConfig.resources
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.BluetoothError
import com.weatherxm.data.models.CountryAndFrequencies
import com.weatherxm.data.models.Frequency
import com.weatherxm.ui.InstantExecutorListener
import com.weatherxm.ui.common.FrequencyState
import com.weatherxm.ui.common.ScannedDevice
import com.weatherxm.ui.common.Status
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.devicesettings.ChangeFrequencyState
import com.weatherxm.ui.devicesettings.FrequencyStatus
import com.weatherxm.usecases.BluetoothConnectionUseCase
import com.weatherxm.usecases.BluetoothScannerUseCase
import com.weatherxm.usecases.StationSettingsUseCase
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
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
class ChangeFrequencyViewModelTest : BehaviorSpec({
    val usecase = mockk<StationSettingsUseCase>()
    val connectionUseCase = mockk<BluetoothConnectionUseCase>()
    val scanUseCase = mockk<BluetoothScannerUseCase>()
    val analytics = mockk<AnalyticsWrapper>()
    lateinit var viewModel: ChangeFrequencyViewModel

    val device = mockk<UIDevice>()

    val lat = 10.0
    val lon = 10.0
    val countryAndFrequencies = CountryAndFrequencies(
        "GR",
        Frequency.EU868,
        listOf(Frequency.US915, Frequency.AU915)
    )
    val recommendedLabel = "Recommended for GR"
    val frequencyState = FrequencyState("GR", listOf("EU868 ($recommendedLabel)", "US915", "AU915"))

    val bondFlow = MutableSharedFlow<Int>(
        replay = 1, onBufferOverflow = BufferOverflow.DROP_LATEST
    )
    val scanFlow = MutableSharedFlow<ScannedDevice>(
        replay = 1, onBufferOverflow = BufferOverflow.DROP_LATEST
    )
    val deviceNotFoundFailure = BluetoothError.DeviceNotFound
    val stationNotInRange = "Station not in range"

    listener(InstantExecutorListener())
    Dispatchers.setMain(StandardTestDispatcher())

    beforeSpec {
        justRun { analytics.trackEventFailure(any()) }
        every { connectionUseCase.registerOnBondStatus() } returns bondFlow
        coEvery { scanUseCase.scan() } returns scanFlow
        coJustRun { connectionUseCase.reboot() }
        coEvery { usecase.getCountryAndFrequencies(lat, lon) } returns countryAndFrequencies
        every { device.getLastCharsOfLabel() } returns "00:00:00"
        every { device.location?.lat } returns lat
        every { device.location?.lon } returns lon

        every {
            resources.getString(R.string.station_not_in_range_subtitle)
        } returns stationNotInRange
        every {
            resources.getString(R.string.recommended_frequency_for, "GR")
        } returns recommendedLabel

        viewModel = ChangeFrequencyViewModel(
            device,
            usecase,
            connectionUseCase,
            scanUseCase,
            resources,
            analytics
        )
    }

    context("Get Country and Frequencies") {
        given("a usecase returning the country and frequencies") {
            runTest { viewModel.getCountryAndFrequencies() }
            then("LiveData onFrequencyState should post the correct FrequencyState") {
                viewModel.onFrequencies().value shouldBe frequencyState
            }
        }
    }

    context("GET / SET the selected frequency") {
        When("GET the selected frequency") {
            then("return the default US915 frequency") {
                viewModel.getSelectedFrequency() shouldBe Frequency.US915.toString()
            }
        }
        When("SET a new selected frequency") {
            viewModel.setSelectedFrequency(0)
            then("GET it to ensure it has been set") {
                viewModel.getSelectedFrequency() shouldBe Frequency.EU868.toString()
            }
        }
    }

    context("Flow when onNotPaired gets triggered") {
        given("the trigger") {
            viewModel.onNotPaired()
            then("onStatus should post an error with the respective ChangeFrequencyState") {
                viewModel.onStatus().value?.status shouldBe Status.ERROR
                viewModel.onStatus().value?.data shouldBe ChangeFrequencyState(
                    FrequencyStatus.PAIR_STATION
                )
            }
        }
    }

    context("Flow when onScanFailure gets triggered") {
        given("a failure") {
            When("it's a BluetoothError.DeviceNotFound") {
                viewModel.onScanFailure(deviceNotFoundFailure)
                then("onStatus should post an error with the respective ChangeFrequencyState") {
                    viewModel.onStatus().value?.status shouldBe Status.ERROR
                    viewModel.onStatus().value?.message shouldBe stationNotInRange
                    viewModel.onStatus().value?.data shouldBe ChangeFrequencyState(
                        FrequencyStatus.SCAN_FOR_STATION, BluetoothError.DeviceNotFound
                    )
                }
            }
            When("it's any other failure") {
                viewModel.onScanFailure(failure)
                then("LiveData onStatus posts an error with the respective ChangeFrequencyState") {
                    viewModel.onStatus().value?.status shouldBe Status.ERROR
                    viewModel.onStatus().value?.data shouldBe ChangeFrequencyState(
                        FrequencyStatus.SCAN_FOR_STATION
                    )
                }
            }
        }
    }

    context("Flow when onConnectionFailure gets triggered") {
        given("the trigger") {
            viewModel.onConnectionFailure(failure)
            then("onStatus should post an error with the respective ChangeFrequencyState") {
                viewModel.onStatus().value?.status shouldBe Status.ERROR
                viewModel.onStatus().value?.data shouldBe ChangeFrequencyState(
                    FrequencyStatus.CONNECT_TO_STATION
                )
            }
        }
    }

    context("Start the changing frequency process when device is connected") {
        given("a usecase returning the result of the change frequency") {
            When("it's a failure") {
                coMockEitherLeft({ connectionUseCase.setFrequency(Frequency.EU868) }, failure)
                runTest { viewModel.onConnected() }
                then("analytics should track the event's failure") {
                    verify(exactly = 1) { analytics.trackEventFailure(any()) }
                }
                then("onStatus should post an error with the respective ChangeFrequencyState") {
                    viewModel.onStatus().value?.status shouldBe Status.ERROR
                    viewModel.onStatus().value?.data shouldBe ChangeFrequencyState(
                        FrequencyStatus.CHANGING_FREQUENCY
                    )
                }
            }
            When("it's a success") {
                coMockEitherRight({ connectionUseCase.setFrequency(Frequency.EU868) }, Unit)
                runTest { viewModel.onConnected() }
                then("onStatus should post a success with the respective ChangeFrequencyState") {
                    viewModel.onStatus().value?.status shouldBe Status.SUCCESS
                    viewModel.onStatus().value?.data shouldBe ChangeFrequencyState(
                        FrequencyStatus.CHANGING_FREQUENCY
                    )
                }
                then("a reboot should take place") {
                    coVerify(exactly = 1) { connectionUseCase.reboot() }
                }
            }
        }
    }

    context("Flow when we want to start the connection process") {
        given("the trigger") {
            runTest { viewModel.startConnectionProcess() }
            then("onStatus should post a success with the respective ChangeFrequencyState") {
                viewModel.onStatus().value?.status shouldBe Status.LOADING
                viewModel.onStatus().value?.data shouldBe ChangeFrequencyState(
                    FrequencyStatus.CONNECT_TO_STATION
                )
            }
        }
    }

    afterSpec {
        Dispatchers.resetMain()
    }
})
