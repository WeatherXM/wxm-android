package com.weatherxm.ui.deviceheliumota

import android.net.Uri
import com.weatherxm.R
import com.weatherxm.TestConfig.failure
import com.weatherxm.TestConfig.resources
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.BluetoothError
import com.weatherxm.data.models.BluetoothOTAState
import com.weatherxm.data.models.Failure.Companion.CODE_BL_OTA_FAILED
import com.weatherxm.data.models.OTAState
import com.weatherxm.ui.InstantExecutorListener
import com.weatherxm.ui.common.Resource
import com.weatherxm.ui.common.Status
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.empty
import com.weatherxm.usecases.BluetoothConnectionUseCase
import com.weatherxm.usecases.BluetoothScannerUseCase
import com.weatherxm.usecases.BluetoothUpdaterUseCase
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
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
class DeviceHeliumOTAViewModelTest : BehaviorSpec({
    val updaterUseCase = mockk<BluetoothUpdaterUseCase>()
    val connectionUseCase = mockk<BluetoothConnectionUseCase>()
    val scannerUseCase = mockk<BluetoothScannerUseCase>()
    val analytics = mockk<AnalyticsWrapper>()
    lateinit var viewModel: DeviceHeliumOTAViewModel

    val firmware = "1.0.0"
    val device = UIDevice(
        "deviceId",
        String.empty(),
        String.empty(),
        null,
        "01:23:45:67:89:AB:CD:DE:EF",
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
        firmware,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null
    )
    val uri = mockk<Uri>()

    val bondFlow = MutableSharedFlow<Int>(
        replay = 1, onBufferOverflow = BufferOverflow.DROP_LATEST
    )
    val updateFlow = MutableSharedFlow<OTAState>(
        replay = 1, onBufferOverflow = BufferOverflow.DROP_LATEST
    )

    val deviceNotFoundFailure = BluetoothError.DeviceNotFound
    val connectionLostFailure = BluetoothError.ConnectionLostException()
    val stationNotInRange = "Station not in range"
    val otaFailed = "OTA Failed"
    val otaAborted = "OTA Aborted"

    listener(InstantExecutorListener())
    Dispatchers.setMain(StandardTestDispatcher())

    beforeSpec {
        justRun { analytics.trackEventFailure(any()) }
        every {
            resources.getString(R.string.station_not_in_range_subtitle)
        } returns stationNotInRange
        every { resources.getString(R.string.error_helium_ota_download_failed) } returns otaFailed
        every { resources.getString(R.string.error_helium_ota_aborted) } returns otaAborted
        every { connectionUseCase.registerOnBondStatus() } returns bondFlow
        every { updaterUseCase.update(uri) } returns updateFlow
        justRun { updaterUseCase.onUpdateSuccess(device.id, firmware) }

        viewModel = DeviceHeliumOTAViewModel(
            device,
            true,
            resources,
            updaterUseCase,
            connectionUseCase,
            scannerUseCase,
            analytics
        )
    }

    fun Resource<State>?.testOnStatusError(message: String?, expectedState: State) {
        this?.status shouldBe Status.ERROR
        this?.message shouldBe message
        this?.data?.status shouldBe expectedState.status
        this?.data?.failure shouldBe expectedState.failure
        this?.data?.otaError shouldBe expectedState.otaError
        this?.data?.otaErrorType shouldBe expectedState.otaErrorType
        this?.data?.otaErrorMessage shouldBe expectedState.otaErrorMessage
    }

    context("Flow when onScanFailure gets triggered") {
        given("a failure") {
            When("it's a BluetoothError.DeviceNotFound") {
                viewModel.onScanFailure(deviceNotFoundFailure)
                then("LiveData onStatus posts the respective error (station not in range)") {
                    viewModel.onStatus().value.testOnStatusError(
                        stationNotInRange,
                        State(OTAStatus.SCAN_FOR_STATION, BluetoothError.DeviceNotFound)
                    )
                }
            }
            When("it's any other failure") {
                viewModel.onScanFailure(failure)
                then("LiveData onStatus posts a generic error") {
                    viewModel.onStatus().value.testOnStatusError(
                        String.empty(),
                        State(OTAStatus.SCAN_FOR_STATION)
                    )
                }
            }
        }
    }

    context("Flow when onNotPaired gets triggered") {
        given("the trigger") {
            viewModel.onNotPaired()
            then("LiveData onStatus posts a generic error") {
                viewModel.onStatus().value.testOnStatusError(
                    String.empty(),
                    State(OTAStatus.PAIR_STATION)
                )
            }
        }
    }

    context("Flow when onConnectionFailure gets triggered") {
        given("the failure") {
            viewModel.onConnectionFailure(connectionLostFailure)
            then("LiveData onStatus posts an error with the Failure's code as a message") {
                viewModel.onStatus().value.testOnStatusError(
                    connectionLostFailure.code,
                    State(OTAStatus.CONNECT_TO_STATION)
                )
            }
        }
    }

    context("Flow when onConnected gets triggered") {
        given("the usecase providing the mechanism for downloading the firmware") {
            When("it's a failure") {
                coMockEitherLeft(
                    { updaterUseCase.downloadFirmwareAndGetFileURI(device.id) },
                    connectionLostFailure
                )
                runTest { viewModel.onConnected() }
                then("LiveData onStatus posts the respective error") {
                    viewModel.onStatus().value.testOnStatusError(
                        otaFailed,
                        State(OTAStatus.DOWNLOADING)
                    )
                }
                then("track the event's failure in analytics") {
                    verify(exactly = 1) { analytics.trackEventFailure(connectionLostFailure.code) }
                }
            }
        }
    }

    context("Start the connection process") {
        given("the usecase providing the mechanism for downloading the firmware") {
            When("it's a success") {
                coMockEitherRight({ updaterUseCase.downloadFirmwareAndGetFileURI(device.id) }, uri)
                When("the current state is BluetoothOTAState.COMPLETED") {
                    updateFlow.tryEmit(OTAState(BluetoothOTAState.COMPLETED, 100))
                    runTest { viewModel.startConnectionProcess() }
                    then("LiveData onStatus should post a success at the correct state") {
                        viewModel.onStatus().isSuccess(State(OTAStatus.INSTALLING))
                    }
                    then("onUpdateSuccess should be called in the usecase to inform our system") {
                        verify(exactly = 1) { updaterUseCase.onUpdateSuccess(device.id, firmware) }
                    }
                }
                When("the current state is BluetoothOTAState.IN_PROGRESS") {
                    updateFlow.tryEmit(OTAState(BluetoothOTAState.IN_PROGRESS, 50))
                    runTest { viewModel.startConnectionProcess() }
                    then("LiveData onInstallingProgress should post the progress value") {
                        viewModel.onInstallingProgress().value shouldBe 50
                    }
                }
                When("the current state is BluetoothOTAState.ABORTED") {
                    updateFlow.tryEmit(OTAState(BluetoothOTAState.ABORTED, 0))
                    runTest { viewModel.startConnectionProcess() }
                    then("LiveData onStatus posts the respective error") {
                        viewModel.onStatus().value.testOnStatusError(
                            otaAborted,
                            State(OTAStatus.INSTALLING)
                        )
                    }
                }
                When("the current state is BluetoothOTAState.FAILED") {
                    val failedOTAState = OTAState(BluetoothOTAState.FAILED, 0, 1, 2, otaFailed)
                    updateFlow.tryEmit(failedOTAState)
                    runTest { viewModel.startConnectionProcess() }
                    then("LiveData onStatus posts the respective error") {
                        viewModel.onStatus().value.testOnStatusError(
                            String.empty(),
                            State(
                                OTAStatus.INSTALLING,
                                otaError = failedOTAState.error,
                                otaErrorType = failedOTAState.errorType,
                                otaErrorMessage = failedOTAState.message
                            )
                        )
                    }
                }
            }
        }
    }

    afterSpec {
        Dispatchers.resetMain()
    }
})
