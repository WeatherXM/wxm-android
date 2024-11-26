package com.weatherxm.ui.claimdevice.helium.result

import com.weatherxm.R
import com.weatherxm.TestConfig.dispatcher
import com.weatherxm.TestConfig.resources
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.BluetoothError
import com.weatherxm.data.models.Frequency
import com.weatherxm.ui.InstantExecutorListener
import com.weatherxm.ui.common.ScannedDevice
import com.weatherxm.ui.common.UIError
import com.weatherxm.usecases.BluetoothConnectionUseCase
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest

class ClaimHeliumResultViewModelTest : BehaviorSpec({
    val usecase = mockk<BluetoothConnectionUseCase>()
    val analytics = mockk<AnalyticsWrapper>()
    lateinit var viewModel: ClaimHeliumResultViewModel

    val scannedDevice = ScannedDevice.empty()
    val frequency = Frequency.EU868
    val devEui = "devEUI"
    val devEuiWithColon = "dev:EUI"
    val claimingKey = "claimingKey"
    val bondFlow = MutableSharedFlow<Int>(
        replay = 1, onBufferOverflow = BufferOverflow.DROP_LATEST
    )

    val bluetoothDisabledFailure = BluetoothError.BluetoothDisabledException()
    val connectionLostFailure = BluetoothError.ConnectionLostException()
    val connectionRejectedFailure = BluetoothError.ConnectionRejectedError()
    val atCommandFailure = BluetoothError.ATCommandError()
    val bluetoothDisabled = "Bluetooth Disabled"
    val connectionLost = "Connection Lost"
    val connectionRejected = "Connection Rejected"
    val setFrequencyFailed = "Set Frequency Failed"
    val rebootFailed = "Reboot Failed"
    val fetchingInfoFailed = "Fetching Info Failed"

    listener(InstantExecutorListener())

    beforeSpec {
        justRun { analytics.trackEventFailure(any()) }

        every { resources.getString(R.string.helium_bluetooth_disabled) } returns bluetoothDisabled
        every { resources.getString(R.string.ble_connection_lost_desc) } returns connectionLost
        every {
            resources.getString(R.string.helium_connection_rejected)
        } returns connectionRejected
        every { resources.getString(R.string.set_frequency_failed_desc) } returns setFrequencyFailed
        every { resources.getString(R.string.helium_reboot_failed) } returns rebootFailed
        every {
            resources.getString(R.string.helium_fetching_info_failed)
        } returns fetchingInfoFailed
        every { usecase.registerOnBondStatus() } returns bondFlow

        viewModel = ClaimHeliumResultViewModel(usecase, resources, analytics, dispatcher)
    }

    fun UIError?.testBLEError(code: String?, message: String) {
        this?.errorCode shouldBe code
        this?.errorMessage shouldBe message
        this?.retryFunction shouldNotBe null
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
                    viewModel.onBLEError().value.testBLEError(null, bluetoothDisabled)
                }
            }
            When("it's ConnectionLostException") {
                viewModel.onConnectionFailure(connectionLostFailure)
                viewModel.onBLEError().value.testBLEError(
                    connectionLostFailure.code,
                    connectionLost
                )
            }
            When("it's any other Failure") {
                viewModel.onConnectionFailure(connectionRejectedFailure)
                viewModel.onBLEError().value.testBLEError(null, connectionRejected)
            }
        }
    }

    context("Flow of setting the frequency") {
        given("a usecase assisting with the flow starting with setting the frequency") {
            When("it's a failure") {
                coMockEitherLeft({ usecase.setFrequency(frequency) }, connectionLostFailure)
                runTest { viewModel.setFrequency(frequency) }
                then("LiveData onBLEError should post the respective error") {
                    viewModel.onBLEError().value.testBLEError(
                        connectionLostFailure.code,
                        setFrequencyFailed
                    )
                }
                then("track the event's failure in analytics") {
                    verify(exactly = 1) { analytics.trackEventFailure(connectionLostFailure.code) }
                }
            }
            When("it's a success start the reboot flow") {
                coMockEitherRight({ usecase.setFrequency(frequency) }, Unit)
                When("it's a failure") {
                    coMockEitherLeft({ usecase.reboot() }, connectionRejectedFailure)
                    runTest { viewModel.setFrequency(frequency) }
                    then("LiveData onBLEError should post the respective error") {
                        viewModel.onBLEError().value.testBLEError(null, rebootFailed)
                    }
                    then("track the event's failure in analytics") {
                        verify(exactly = 1) {
                            analytics.trackEventFailure(connectionRejectedFailure.code)
                        }
                    }
                }
                When("it's a success start the connect flow") {
                    coMockEitherRight({ usecase.reboot() }, Unit)
                    runTest { viewModel.setFrequency(frequency) }
                    then("LiveData onBLEConnection should post a true value") {
                        viewModel.onBLEConnection().value shouldBe true
                    }
                }
            }
        }
    }

    context("Flow after we have connected to the device (fetch Dev EUI and claiming key)") {
        given("a usecase providing the Dev EUI") {
            When("it's a failure") {
                coMockEitherLeft({ usecase.fetchDeviceEUI() }, bluetoothDisabledFailure)
                runTest { viewModel.onConnected() }
                then("LiveData onBLEError should post the respective error") {
                    viewModel.onBLEError().value.testBLEError(null, fetchingInfoFailed)
                }
                then("track the event's failure in analytics") {
                    verify(exactly = 1) {
                        analytics.trackEventFailure(bluetoothDisabledFailure.code)
                    }
                }
            }
            When("it's a success") {
                coMockEitherRight({ usecase.fetchDeviceEUI() }, devEuiWithColon)
                coMockEitherLeft({ usecase.fetchClaimingKey() }, atCommandFailure)
                runTest { viewModel.onConnected() }
                then("LiveData onBLEDevEUI should post the Dev EUI unmasked (without `:`)") {
                    viewModel.onBLEDevEUI().value shouldBe devEui
                }
                When("fetching the claiming key is a failure") {
                    then("LiveData onBLEError should post the respective error") {
                        viewModel.onBLEError().value.testBLEError(null, fetchingInfoFailed)
                    }
                    then("track the event's failure in analytics") {
                        verify(exactly = 1) {
                            analytics.trackEventFailure(atCommandFailure.code)
                        }
                    }
                }
                When("fetching the claiming key is a success") {
                    coMockEitherRight({ usecase.fetchClaimingKey() }, claimingKey)
                    runTest { viewModel.onConnected() }
                    then("LiveData onBLEClaimingKey should post the claiming key") {
                        viewModel.onBLEClaimingKey().value shouldBe claimingKey
                    }
                }
            }
        }
    }
})
