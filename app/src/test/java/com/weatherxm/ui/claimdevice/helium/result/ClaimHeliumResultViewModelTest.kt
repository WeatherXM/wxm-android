package com.weatherxm.ui.claimdevice.helium.result

import com.weatherxm.R
import com.weatherxm.TestConfig.resources
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.BluetoothError
import com.weatherxm.data.models.Frequency
import com.weatherxm.ui.InstantExecutorListener
import com.weatherxm.ui.common.ScannedDevice
import com.weatherxm.usecases.BluetoothConnectionUseCase
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class ClaimHeliumResultViewModelTest : BehaviorSpec({
    val usecase = mockk<BluetoothConnectionUseCase>()
    val analytics = mockk<AnalyticsWrapper>()
    lateinit var viewModel: ClaimHeliumResultViewModel

    val scannedDevice = mockk<ScannedDevice>()
    val frequency = Frequency.EU868
    val devEui = "devEUI"
    val claimingKey = "claimingKey"

    val bluetoothDisabledFailure = BluetoothError.BluetoothDisabledException()
    val connectionLostFailure = BluetoothError.ConnectionLostException()
    val connectionRejectedFailure = BluetoothError.ConnectionRejectedError()
    val bluetoothDisabled = "Bluetooth Disabled"
    val connectionLost = "Connection Lost"
    val connectionRejected = "Connection Rejected"
    val setFrequencyFailed = "Set Frequency Failed"
    val rebootFailed = "Reboot Failed"
    val fetchingInfoFailed = "Fetching Info Failed"

    listener(InstantExecutorListener())
    Dispatchers.setMain(StandardTestDispatcher())

    beforeSpec {
        justRun { analytics.trackEventFailure(any()) }

        every { resources.getString(R.string.helium_bluetooth_disabled) } returns bluetoothDisabled
        every { resources.getString(R.string.ble_connection_lost_desc) } returns connectionLost
        every { resources.getString(R.string.helium_connection_rejected) } returns connectionRejected
        every { resources.getString(R.string.set_frequency_failed_desc) } returns setFrequencyFailed
        every { resources.getString(R.string.helium_reboot_failed) } returns rebootFailed
        every { resources.getString(R.string.helium_fetching_info_failed) } returns fetchingInfoFailed

        viewModel = ClaimHeliumResultViewModel(usecase, resources, analytics)
    }

    context("SET and then GET the scanned device") {
        given("a scanned device") {
            then("ensure that the device is not currently set") {
                viewModel.scannedDevice() shouldBe ScannedDevice.empty()
            }
            then("SET it") {
                viewModel.setSelectedDevice(scannedDevice)
            }
            then("GET it to ensure it has been set correctly") {
                viewModel.scannedDevice() shouldBe scannedDevice
            }
        }
    }

    context("Handle connection failure") {
        given("a Failure") {
            When("it's BluetoothDisabledException") {
                viewModel.onConnectionFailure(bluetoothDisabledFailure)
                then("onBLEError should post the respective error") {
                    viewModel.onBLEError().value?.errorCode shouldBe bluetoothDisabledFailure.code
                    viewModel.onBLEError().value?.errorMessage shouldBe bluetoothDisabled
                    viewModel.onBLEError().value?.retryFunction shouldNotBe null
                }
            }
            When("it's ConnectionLostException") {
                viewModel.onConnectionFailure(connectionLostFailure)
                viewModel.onBLEError().value?.errorCode shouldBe connectionLostFailure.code
                viewModel.onBLEError().value?.errorMessage shouldBe connectionLost
                viewModel.onBLEError().value?.retryFunction shouldNotBe null
            }
            When("it's any other Failure") {
                viewModel.onConnectionFailure(connectionRejectedFailure)
                viewModel.onBLEError().value?.errorCode shouldBe connectionRejectedFailure.code
                viewModel.onBLEError().value?.errorMessage shouldBe connectionRejected
                viewModel.onBLEError().value?.retryFunction shouldNotBe null
            }
        }
    }

    afterSpec {
        Dispatchers.resetMain()
    }
})
