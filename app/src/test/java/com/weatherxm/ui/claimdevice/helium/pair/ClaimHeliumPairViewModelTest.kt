package com.weatherxm.ui.claimdevice.helium.pair

import com.weatherxm.R
import com.weatherxm.TestConfig.failure
import com.weatherxm.TestConfig.resources
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.BluetoothError
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.Failure.Companion.CODE_BL_DEVICE_NOT_PAIRED
import com.weatherxm.ui.InstantExecutorListener
import com.weatherxm.ui.common.ScannedDevice
import com.weatherxm.usecases.BluetoothConnectionUseCase
import com.weatherxm.usecases.BluetoothScannerUseCase
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
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
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class ClaimHeliumPairViewModelTest : BehaviorSpec({
    val usecase = mockk<BluetoothConnectionUseCase>()
    val scannerUseCase = mockk<BluetoothScannerUseCase>()
    val analytics = mockk<AnalyticsWrapper>()
    lateinit var viewModel: ClaimHeliumPairViewModel

    val scannedDevice = mockk<ScannedDevice>()

    val bluetoothDisabledFailure = BluetoothError.BluetoothDisabledException()
    val connectionLostFailure = BluetoothError.ConnectionLostException()
    val connectionRejectedFailure = BluetoothError.ConnectionRejectedError()
    val bluetoothDisabled = "Bluetooth Disabled"
    val pairingFailed = "Pairing Failed"

    val bondFlow = MutableSharedFlow<Int>(
        replay = 1, onBufferOverflow = BufferOverflow.DROP_LATEST
    )
    val scanFlow = MutableSharedFlow<ScannedDevice>(
        replay = 1, onBufferOverflow = BufferOverflow.DROP_LATEST
    )

    listener(InstantExecutorListener())
    Dispatchers.setMain(StandardTestDispatcher())

    beforeSpec {
        every { resources.getString(R.string.helium_bluetooth_disabled) } returns bluetoothDisabled
        every { resources.getString(R.string.helium_pairing_failed_desc) } returns pairingFailed
        justRun { analytics.trackEventFailure(CODE_BL_DEVICE_NOT_PAIRED) }
        coMockEitherLeft({ usecase.setPeripheral(scannedDevice.address) }, failure)
        coEvery { scannerUseCase.scan() } returns scanFlow
        every { usecase.registerOnBondStatus() } returns bondFlow

        viewModel = ClaimHeliumPairViewModel(resources, analytics, scannerUseCase, usecase)
    }

    /**
     * TODO: scanBleDevices is missing due to the issue on CountDownTimer, we need to move this
     * functionality in an util class or sth so we can mock it in the tests.
     */

//    context("Scan devices") {
//        given("a usecase providing us with the scanned devices") {
//            scanFlow.tryEmit(scannedDevice)
//            runTest { viewModel.scanBleDevices() }
//            then("LiveData onNewScannedDevice should post the newly scanned device") {
//                viewModel.onNewScannedDevice().value?.size shouldBe 1
//                viewModel.onNewScannedDevice().value?.get(0) shouldBe scannedDevice
//            }
//        }
//    }

    context("Flow when onNotPaired gets triggered") {
        given("the trigger") {
            viewModel.onNotPaired()
            then("we should log the event's failure in analytics") {
                verify(exactly = 1) {
                    analytics.trackEventFailure(Failure.CODE_BL_DEVICE_NOT_PAIRED)
                }
            }
            then("onBLEError should post the respective error") {
                viewModel.onBLEError().value?.errorMessage shouldBe pairingFailed
                viewModel.onBLEError().value?.retryFunction shouldNotBe null
            }
        }
    }

    context("Flow when onConnected gets triggered") {
        given("the trigger") {
            viewModel.onConnected()
            then("LiveData onBLEConnection should post a true value") {
                viewModel.onBLEConnection().value shouldBe true
            }
        }
    }

    context("Handle connection failure") {
        given("a Failure") {
            When("it's BluetoothDisabledException") {
                viewModel.onConnectionFailure(bluetoothDisabledFailure)
                then("onBLEError should post the respective error") {
                    viewModel.onBLEError().value?.errorMessage shouldBe bluetoothDisabled
                    viewModel.onBLEError().value?.retryFunction shouldNotBe null
                }
            }
            When("it's ConnectionLostException") {
                viewModel.onConnectionFailure(connectionLostFailure)
                then("LiveData onBLEConnectionLost should post the value true")
                viewModel.onBLEConnectionLost().value shouldBe true
            }
            When("it's any other Failure") {
                viewModel.onConnectionFailure(connectionRejectedFailure)
                viewModel.onBLEError().value?.errorMessage shouldBe pairingFailed
                viewModel.onBLEError().value?.retryFunction shouldNotBe null
            }
        }
    }

    context("Setup bluetooth claiming flow") {
        given("a scanned device") {
            viewModel.setupBluetoothClaiming(scannedDevice)
            then("perform the required actions and set the new scanned device") {
                viewModel.scannedDevice() shouldBe scannedDevice
            }
        }
    }

    afterSpec {
        Dispatchers.resetMain()
    }
})
