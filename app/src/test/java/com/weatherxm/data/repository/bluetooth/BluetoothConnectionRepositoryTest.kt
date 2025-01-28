package com.weatherxm.data.repository.bluetooth

import android.bluetooth.BluetoothDevice
import com.weatherxm.TestConfig.failure
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isError
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.data.datasource.DeviceFrequencyDataSource
import com.weatherxm.data.datasource.bluetooth.BluetoothConnectionDataSource
import com.weatherxm.data.models.Frequency
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow

class BluetoothConnectionRepositoryTest : BehaviorSpec({
    lateinit var dataSource: BluetoothConnectionDataSource
    lateinit var deviceFrequencyDataSource: DeviceFrequencyDataSource
    lateinit var repository: BluetoothConnectionRepository

    val pairedDevices = listOf<BluetoothDevice>(mockk())
    val bondStatusFlow = mockk<Flow<Int>>()
    val macAddress = "macAddress"
    val frequency = Frequency.EU868
    val testClaimingKey = "testClaimingKey"
    val testDeviceEUI = "testDeviceEUI"

    beforeContainer {
        dataSource = mockk<BluetoothConnectionDataSource>()
        deviceFrequencyDataSource = mockk<DeviceFrequencyDataSource>()
        repository = BluetoothConnectionRepositoryImpl(dataSource, deviceFrequencyDataSource)
        coEvery { dataSource.getPairedDevices() } returns pairedDevices
        coEvery { dataSource.registerOnBondStatus() } returns bondStatusFlow
        coJustRun { dataSource.disconnectFromPeripheral() }
    }

    given("Some paired devices") {
        then("The repository should return them") {
            repository.getPairedDevices() shouldBe pairedDevices
        }
    }

    context("Get the Bond Status Flow") {
        given("The data source providing the flow") {
            then("The repository should return that flow") {
                repository.registerOnBondStatus() shouldBe bondStatusFlow
            }
        }
    }

    context("Perform peripheral related actions") {
        given("A data source exposing peripheral related actions") {
            and("Set Peripheral") {
                When("success") {
                    coMockEitherRight({ dataSource.setPeripheral(macAddress) }, Unit)
                    repository.setPeripheral(macAddress).isSuccess(Unit)
                }
                When("failure") {
                    coMockEitherLeft({ dataSource.setPeripheral(macAddress) }, failure)
                    repository.setPeripheral(macAddress).isError()
                }
            }
            and("Connect to Peripheral") {
                When("success") {
                    coMockEitherRight({ dataSource.connectToPeripheral() }, Unit)
                    repository.connectToPeripheral().isSuccess(Unit)
                }
                When("failure") {
                    coMockEitherLeft({ dataSource.connectToPeripheral() }, failure)
                    repository.connectToPeripheral().isError()
                }
            }
            and("Disconnect from Peripheral") {
                repository.disconnectFromPeripheral()
                coVerify(exactly = 1) { dataSource.disconnectFromPeripheral() }
            }
            and("Fetch Claiming Key") {
                When("success") {
                    coMockEitherRight({ dataSource.fetchClaimingKey() }, testClaimingKey)
                    repository.fetchClaimingKey().isSuccess(testClaimingKey)
                }
                When("failure") {
                    coMockEitherLeft({ dataSource.fetchClaimingKey() }, failure)
                    repository.fetchClaimingKey().isError()
                }
            }
            and("Set Frequency") {
                When("success") {
                    and("Fetch Device EUI fails") {
                        coMockEitherRight({ dataSource.setFrequency(frequency) }, Unit)
                        coMockEitherLeft({ dataSource.fetchDeviceEUI() }, failure)
                        repository.setFrequency(frequency).isError()
                    }
                    and("Fetch Device EUI is a success") {
                        and("Setting the Device EUI through datasource is a failure") {
                            coMockEitherRight({ dataSource.setFrequency(frequency) }, Unit)
                            coMockEitherRight({ dataSource.fetchDeviceEUI() }, testDeviceEUI)
                            coMockEitherLeft(
                                {
                                    deviceFrequencyDataSource.setDeviceFrequency(
                                        testDeviceEUI, frequency.name
                                    )
                                },
                                failure
                            )
                            repository.setFrequency(frequency).isError()
                        }
                        and("Setting the Device EUI through datasource is a success") {
                            coMockEitherRight({ dataSource.setFrequency(frequency) }, Unit)
                            coMockEitherRight({ dataSource.fetchDeviceEUI() }, testDeviceEUI)
                            coMockEitherRight(
                                {
                                    deviceFrequencyDataSource.setDeviceFrequency(
                                        testDeviceEUI, frequency.name
                                    )
                                },
                                Unit
                            )
                            then("return Unit as success") {
                                repository.setFrequency(frequency).isSuccess(Unit)
                            }
                        }
                    }
                }
                When("failure") {
                    coMockEitherLeft({ dataSource.setFrequency(frequency) }, failure)
                    repository.setFrequency(frequency).isError()
                }
            }
            and("Reboot") {
                When("success") {
                    coMockEitherRight({ dataSource.reboot() }, Unit)
                    repository.reboot().isSuccess(Unit)
                    then("Disconnect from the peripheral") {
                        coVerify(exactly = 1) { dataSource.disconnectFromPeripheral() }
                    }
                }
                When("failure") {
                    coMockEitherLeft({ dataSource.reboot() }, failure)
                    repository.reboot().isError()
                }
            }

        }
    }
})
