package com.weatherxm.data.datasource.bluetooth

import android.bluetooth.BluetoothDevice
import arrow.core.Either
import com.weatherxm.TestConfig.failure
import com.weatherxm.TestUtils.isError
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.data.bluetooth.BluetoothConnectionManager
import com.weatherxm.data.bluetooth.BluetoothConnectionManager.Companion.AT_CLAIMING_KEY_COMMAND
import com.weatherxm.data.bluetooth.BluetoothConnectionManager.Companion.AT_DEV_EUI_COMMAND
import com.weatherxm.data.bluetooth.BluetoothConnectionManager.Companion.AT_SET_FREQUENCY_COMMAND
import com.weatherxm.data.datasource.bluetooth.BluetoothConnectionDataSourceImpl.Companion.CLAIM_MAX_RETRIES
import com.weatherxm.data.models.BluetoothError
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.Frequency
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.Flow

class BluetoothConnectionDataSourceTest : BehaviorSpec({
    val connectionManager = mockk<BluetoothConnectionManager>()
    val datasource = BluetoothConnectionDataSourceImpl(connectionManager)

    val bluetoothDevices = listOf<BluetoothDevice>(mockk())
    val address = "address"
    val bondStatusFlow = mockk<Flow<Int>>()
    val claimingKey = "claimingKey"
    val devEui = "devEui"
    val europeFrequency = Frequency.EU868
    val frequencyCommand = "${AT_SET_FREQUENCY_COMMAND}5\r\n"
    val fetchSlot = slot<(Either<Failure, String>) -> Unit>()
    val unitSlot = slot<(Either<Failure, Unit>) -> Unit>()

    beforeSpec {
        every { connectionManager.getPairedDevices() } returns bluetoothDevices
        coJustRun { connectionManager.disconnectFromPeripheral() }
        coEvery { connectionManager.onBondStatus() } returns bondStatusFlow
    }

    context("Get paired devices") {
        When("Using the connection manager") {
            then("return the paired devices") {
                datasource.getPairedDevices() shouldBe bluetoothDevices
            }
        }
    }

    context("Set Peripheral") {
        When("Using the connection manager") {
            When("it's a failure") {
                coEvery { connectionManager.setPeripheral(address) } returns Either.Left(failure)
                then("return the failure") {
                    datasource.setPeripheral(address).isError()
                }
            }
            When("it's a success") {
                coEvery { connectionManager.setPeripheral(address) } returns Either.Right(Unit)
                then("return the Unit as a success") {
                    datasource.setPeripheral(address).isSuccess(Unit)
                }
            }
        }
    }

    context("Connect to peripheral") {
        When("Using the connection manager") {
            and("it's a ConnectionLostException failure") {
                coEvery {
                    connectionManager.connectToPeripheral()
                } returns Either.Left(BluetoothError.ConnectionLostException())
                then("retry until [CLAIM_MAX_RETRIES = $CLAIM_MAX_RETRIES] is hit") {
                    datasource.connectToPeripheral()
                    coVerify(exactly = CLAIM_MAX_RETRIES + 1) {
                        connectionManager.connectToPeripheral()
                    }
                }
            }
            and("it's a failure other than ConnectionLostException") {
                coEvery {
                    connectionManager.connectToPeripheral()
                } returns Either.Left(BluetoothError.ConnectionRejectedError())
                then("return that failure") {
                    datasource.connectToPeripheral().leftOrNull()
                        .shouldBeTypeOf<BluetoothError.ConnectionRejectedError>()
                }
            }
            and("it's a success") {
                coEvery { connectionManager.connectToPeripheral() } returns Either.Right(Unit)
                then("return Unit as a success") {
                    datasource.connectToPeripheral().isSuccess(Unit)
                }
            }
        }
    }

    context("Disconnect from peripheral") {
        When("Using the connection manager") {
            then("ensure that the disconnect functionality in connection manager is called") {
                datasource.disconnectFromPeripheral()
                coVerify(exactly = 1) { connectionManager.disconnectFromPeripheral() }
            }
        }
    }

    context("Get the bond status flow") {
        When("Using the connection manager") {
            then("return that bond status flow") {
                datasource.registerOnBondStatus() shouldBe bondStatusFlow
            }
        }
    }

    context("Fetch claiming key") {
        When("Using the connection manager") {
            and("it's a success") {
                coEvery {
                    connectionManager.fetchATCommand(AT_CLAIMING_KEY_COMMAND, capture(fetchSlot))
                }.answers {
                    fetchSlot.captured.invoke(Either.Right(claimingKey))
                }
                then("return the claiming key") {
                    datasource.fetchClaimingKey().isSuccess(claimingKey)
                }
            }
            and("it's a failure") {
                coEvery {
                    connectionManager.fetchATCommand(AT_CLAIMING_KEY_COMMAND, capture(fetchSlot))
                }.answers {
                    fetchSlot.captured.invoke(Either.Left(failure))
                }
                then("return the failure") {
                    datasource.fetchClaimingKey().isError()
                }
            }
        }
    }

    context("Fetch device EUI") {
        When("Using the connection manager") {
            and("it's a success") {
                coEvery {
                    connectionManager.fetchATCommand(AT_DEV_EUI_COMMAND, capture(fetchSlot))
                }.answers {
                    fetchSlot.captured.invoke(Either.Right(devEui))
                }
                then("return the claiming key") {
                    datasource.fetchDeviceEUI().isSuccess(devEui)
                }
            }
            and("it's a failure") {
                coEvery {
                    connectionManager.fetchATCommand(AT_DEV_EUI_COMMAND, capture(fetchSlot))
                }.answers {
                    fetchSlot.captured.invoke(Either.Left(failure))
                }
                then("return the failure") {
                    datasource.fetchDeviceEUI().isError()
                }
            }
        }
    }

    context("Set the frequency in device") {
        When("Using the connection manager") {
            and("it's a success") {
                coEvery {
                    connectionManager.reboot(capture(unitSlot))
                }.answers {
                    unitSlot.captured.invoke(Either.Right(Unit))
                }
                then("return the claiming key") {
                    datasource.reboot().isSuccess(Unit)
                }
            }
            and("it's a failure") {
                coEvery {
                    connectionManager.reboot(capture(unitSlot))
                }.answers {
                    unitSlot.captured.invoke(Either.Left(failure))
                }
                then("return the failure") {
                    datasource.reboot().isError()
                }
            }
        }
    }

    context("Reboot the device") {
        When("Using the connection manager") {
            and("it's a success") {
                coEvery {
                    connectionManager.setATCommand(frequencyCommand, capture(unitSlot))
                }.answers {
                    unitSlot.captured.invoke(Either.Right(Unit))
                }
                then("return the claiming key") {
                    datasource.setFrequency(europeFrequency).isSuccess(Unit)
                }
            }
            and("it's a failure") {
                coEvery {
                    connectionManager.setATCommand(frequencyCommand, capture(unitSlot))
                }.answers {
                    unitSlot.captured.invoke(Either.Left(failure))
                }
                then("return the failure") {
                    datasource.setFrequency(europeFrequency).isError()
                }
            }
        }
    }
})
