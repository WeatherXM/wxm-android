package com.weatherxm.usecases

import android.bluetooth.BluetoothDevice
import com.weatherxm.TestConfig.failure
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isError
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.data.Frequency
import com.weatherxm.data.repository.bluetooth.BluetoothConnectionRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow

class BluetoothConnectionUseCaseTest : BehaviorSpec({
    val repository: BluetoothConnectionRepository = mockk()
    val usecase: BluetoothConnectionUseCase = BluetoothConnectionUseCaseImpl(repository)

    val pairedDevices = listOf<BluetoothDevice>(mockk())
    val bondStatusFlow = mockk<Flow<Int>>()
    val macAddress = "macAddress"
    val frequency = Frequency.EU868
    val testClaimingKey = "testClaimingKey"
    val testDeviceEUI = "testDeviceEUI"

    beforeSpec {
        coEvery { repository.getPairedDevices() } returns pairedDevices
        coEvery { repository.registerOnBondStatus() } returns bondStatusFlow
        coJustRun { repository.disconnectFromPeripheral() }
    }

    context("Get the  paired devices") {
        given("The repository providing them") {
            then("The usecase should return them") {
                usecase.getPairedDevices() shouldBe pairedDevices
            }
        }
    }

    context("Get the Bond Status Flow") {
        given("The repository providing the flow") {
            then("The usecase should return that flow") {
                usecase.registerOnBondStatus() shouldBe bondStatusFlow
            }
        }
    }

    context("Perform peripheral related actions") {
        given("A repository exposing peripheral related actions") {
            and("Set Peripheral") {
                When("success") {
                    coMockEitherRight({ repository.setPeripheral(macAddress) }, Unit)
                    usecase.setPeripheral(macAddress).isSuccess(Unit)
                }
                When("failure") {
                    coMockEitherLeft({ repository.setPeripheral(macAddress) }, failure)
                    usecase.setPeripheral(macAddress).isError()
                }
            }
            and("Connect to Peripheral") {
                When("success") {
                    coMockEitherRight({ repository.connectToPeripheral() }, Unit)
                    usecase.connectToPeripheral().isSuccess(Unit)
                }
                When("failure") {
                    coMockEitherLeft({ repository.connectToPeripheral() }, failure)
                    usecase.connectToPeripheral().isError()
                }
            }
            and("Disconnect from Peripheral") {
                usecase.disconnectFromPeripheral()
                coVerify(exactly = 1) { repository.disconnectFromPeripheral() }
            }
            and("Fetch Claiming Key") {
                When("success") {
                    coMockEitherRight({ repository.fetchClaimingKey() }, testClaimingKey)
                    usecase.fetchClaimingKey().isSuccess(testClaimingKey)
                }
                When("failure") {
                    coMockEitherLeft({ repository.fetchClaimingKey() }, failure)
                    usecase.fetchClaimingKey().isError()
                }
            }
            and("Fetch Device EUI") {
                When("success") {
                    coMockEitherRight({ repository.fetchDeviceEUI() }, testDeviceEUI)
                    usecase.fetchDeviceEUI().isSuccess(testDeviceEUI)
                }
                When("failure") {
                    coMockEitherLeft({ repository.fetchClaimingKey() }, failure)
                    usecase.fetchClaimingKey().isError()
                }
            }
            and("Set Frequency") {
                When("success") {
                    coMockEitherRight({ repository.setFrequency(frequency) }, Unit)
                    usecase.setFrequency(frequency).isSuccess(Unit)
                }
                When("failure") {
                    coMockEitherLeft({ repository.setFrequency(frequency) }, failure)
                    usecase.setFrequency(frequency).isError()
                }
            }
            and("Reboot") {
                When("success") {
                    coMockEitherRight({ repository.reboot() }, Unit)
                    usecase.reboot().isSuccess(Unit)
                }
                When("failure") {
                    coMockEitherLeft({ repository.reboot() }, failure)
                    usecase.reboot().isError()
                }
            }
        }
    }
})
