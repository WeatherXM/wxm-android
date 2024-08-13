package com.weatherxm.data.repository

import com.weatherxm.TestConfig.failure
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isError
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.data.Attributes
import com.weatherxm.data.Device
import com.weatherxm.data.DeviceInfo
import com.weatherxm.data.Hex
import com.weatherxm.data.Location
import com.weatherxm.data.Relation
import com.weatherxm.data.datasource.CacheAddressDataSource
import com.weatherxm.data.datasource.CacheDeviceDataSource
import com.weatherxm.data.datasource.CacheFollowDataSource
import com.weatherxm.data.datasource.NetworkAddressDataSource
import com.weatherxm.data.datasource.NetworkDeviceDataSource
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk

class DeviceRepositoryTest : BehaviorSpec({
    lateinit var networkDeviceSource: NetworkDeviceDataSource
    lateinit var cacheDeviceSource: CacheDeviceDataSource
    lateinit var networkAddressSource: NetworkAddressDataSource
    lateinit var cacheAddressSource: CacheAddressDataSource
    lateinit var cacheFollowSource: CacheFollowDataSource
    lateinit var repo: DeviceRepository

    val ownedId = "ownedId"
    val followedId = "followedId"
    val ownedDevice = Device(
        ownedId,
        "",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        Relation.owned,
        null
    )
    val followedDevice = Device(
        followedId,
        "",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        Relation.followed,
        null
    )
    val devices = mutableListOf(ownedDevice, followedDevice)
    val serialNumber = "serialNumber"
    val location = Location.empty()
    val deviceInfo = mockk<DeviceInfo>()
    val friendlyName = "My Weather Station"
    val hexIndex = "hexIndex"
    val address = "Device Address"
    val deviceWithHexInfo = Device(
        ownedId,
        "",
        null,
        null,
        null,
        null,
        Attributes(null, null, null, null, null, mockk(), Hex(hexIndex, emptyArray(), location)),
        null,
        null,
        null,
        Relation.owned,
        null
    )

    beforeContainer {
        networkDeviceSource = mockk()
        cacheDeviceSource = mockk()
        networkAddressSource = mockk()
        cacheAddressSource = mockk()
        cacheFollowSource = mockk()
        repo = DeviceRepositoryImpl(
            networkDeviceSource,
            cacheDeviceSource,
            networkAddressSource,
            cacheAddressSource,
            cacheFollowSource
        )
        coJustRun { cacheDeviceSource.setUserDevicesIds(any()) }
        coJustRun { cacheFollowSource.setFollowedDevicesIds(any()) }
        coJustRun { cacheAddressSource.setLocationAddress(hexIndex, address) }
        coEvery { cacheDeviceSource.getUserDevicesIds() } returns emptyList()
        coMockEitherLeft(
            { cacheAddressSource.getLocationAddress(hexIndex, location) },
            failure
        )
    }

    context("Get user devices") {
        given("A response containing user devices") {
            When("the response is a failure") {
                coMockEitherLeft({ networkDeviceSource.getUserDevices() }, failure)
                then("return that failure") {
                    repo.getUserDevices().isError()
                }
            }
            When("the response is a success") {
                coMockEitherRight({ networkDeviceSource.getUserDevices() }, devices)
                val response = repo.getUserDevices()
                then("get address of devices based on their hex7 (which is null on Devices") {
                    response.onRight {
                        it.onEach { device ->
                            device.address shouldBe null
                        }
                    }
                }
                then("save owned devices in cache") {
                    coVerify(exactly = 1) { cacheDeviceSource.setUserDevicesIds(listOf(ownedId)) }
                }
                then("save followed devices in cache") {
                    coVerify(exactly = 1) {
                        cacheFollowSource.setFollowedDevicesIds(listOf(followedId))
                    }
                }
                then("return that success") {
                    response.isSuccess(devices)
                }
            }

        }

        given("A deviceID") {
            and("a response containing this device") {
                When("the response is a failure") {
                    coMockEitherLeft({ networkDeviceSource.getUserDevice(ownedId) }, failure)
                    then("return that failure") {
                        repo.getUserDevice(ownedId).isError()
                    }
                }
                When("the response is a success") {
                    coMockEitherRight({ networkDeviceSource.getUserDevice(ownedId) }, ownedDevice)
                    val response = repo.getUserDevice(ownedId)
                    then("get the address based on its hex7 (which is null on this Device") {
                        response.onRight {
                            it.address shouldBe null
                        }
                    }
                    then("return that success") {
                        response.isSuccess(ownedDevice)
                    }
                }
            }
        }
    }

    context("Claim a device") {
        given("A response containing the claiming result") {
            When("the response is a failure") {
                coMockEitherLeft(
                    { networkDeviceSource.claimDevice(serialNumber, location) },
                    failure
                )
                then("return that failure") {
                    repo.claimDevice(serialNumber, location).isError()
                }
            }
            When("the response is a success") {
                coMockEitherRight(
                    { networkDeviceSource.claimDevice(serialNumber, location) },
                    ownedDevice
                )
                then("return that success") {
                    repo.claimDevice(serialNumber, location).isSuccess(ownedDevice)
                }
                then("save this device along with the rest owned devices in cache") {
                    coVerify(exactly = 1) { cacheDeviceSource.getUserDevicesIds() }
                    coVerify(exactly = 1) { cacheDeviceSource.setUserDevicesIds(listOf(ownedId)) }
                }
            }

        }
    }

    context("Remove a device") {
        given("A response containing the removal result") {
            When("the response is a failure") {
                coMockEitherLeft({ networkDeviceSource.removeDevice(serialNumber) }, failure)
                then("return that failure") {
                    repo.removeDevice(serialNumber, ownedId).isError()
                }
            }
            When("the response is a success") {
                coMockEitherRight({ networkDeviceSource.removeDevice(serialNumber) }, Unit)
                then("return that success") {
                    repo.removeDevice(serialNumber, ownedId).isSuccess(Unit)
                }
                then("remove this device from the rest owned devices in cache") {
                    coEvery { cacheDeviceSource.getUserDevicesIds() } returns listOf(ownedId)
                    coVerify(exactly = 1) { cacheDeviceSource.getUserDevicesIds() }
                    coVerify(exactly = 1) { cacheDeviceSource.setUserDevicesIds(listOf()) }
                }
            }

        }
    }

    context("Set or clear a friendly name") {
        given("a deviceID") {
            and("a friendly name") {
                and("a response containing the set friendly name result") {
                    When("the response is a failure") {
                        coMockEitherLeft(
                            { networkDeviceSource.setFriendlyName(serialNumber, friendlyName) },
                            failure
                        )
                        then("return that failure") {
                            repo.setFriendlyName(serialNumber, friendlyName).isError()
                        }
                    }
                    When("the response is a success") {
                        coMockEitherRight(
                            { networkDeviceSource.setFriendlyName(serialNumber, friendlyName) },
                            Unit
                        )
                        then("return that success") {
                            repo.setFriendlyName(serialNumber, friendlyName).isSuccess(Unit)
                        }
                    }
                }
            }
            and("a response containing the clear friendly name result") {
                When("the response is a failure") {
                    coMockEitherLeft(
                        { networkDeviceSource.clearFriendlyName(serialNumber) },
                        failure
                    )
                    then("return that failure") {
                        repo.clearFriendlyName(serialNumber).isError()
                    }
                }
                When("the response is a success") {
                    coMockEitherRight({ networkDeviceSource.clearFriendlyName(serialNumber) }, Unit)
                    then("return that success") {
                        repo.clearFriendlyName(serialNumber).isSuccess(Unit)
                    }
                }

            }
        }
    }

    context("Get device info") {
        given("A response containing the info result") {
            When("the response is a failure") {
                coMockEitherLeft({ networkDeviceSource.getDeviceInfo(ownedId) }, failure)
                then("return that failure") {
                    repo.getDeviceInfo(ownedId).isError()
                }
            }
            When("the response is a success") {
                coMockEitherRight({ networkDeviceSource.getDeviceInfo(ownedId) }, deviceInfo)
                then("return that success") {
                    repo.getDeviceInfo(ownedId).isSuccess(deviceInfo)
                }
            }
        }
    }

    context("Set a device location") {
        given("A response containing the set location result") {
            When("the response is a failure") {
                coMockEitherLeft(
                    { networkDeviceSource.setLocation(serialNumber, 0.0, 0.0) },
                    failure
                )
                then("return that failure") {
                    repo.setLocation(serialNumber, 0.0, 0.0).isError()
                }
            }
            When("the response is a success") {
                coMockEitherRight(
                    { networkDeviceSource.setLocation(serialNumber, 0.0, 0.0) },
                    ownedDevice
                )
                then("return that success") {
                    repo.setLocation(serialNumber, 0.0, 0.0).isSuccess(ownedDevice)
                }
            }

        }
    }

    context("Get device address from Hex7 info") {
        given("a device with Hex7 info") {
            When("we have in cache the address of this hex") {
                coMockEitherRight(
                    { cacheAddressSource.getLocationAddress(hexIndex, location) },
                    address
                )
                then("return that address") {
                    repo.getDeviceAddress(deviceWithHexInfo) shouldBe address
                }
            }
            When("we don't have in cache the address of this hex so we fetch it from network") {
                When("the response is a failure") {
                    coMockEitherLeft(
                        { networkAddressSource.getLocationAddress(hexIndex, location) },
                        failure
                    )
                    then("return that failure") {
                        repo.getDeviceAddress(deviceWithHexInfo) shouldBe null
                    }
                }
                When("the response is a success") {
                    coMockEitherRight(
                        { networkAddressSource.getLocationAddress(hexIndex, location) },
                        address
                    )
                    then("return that success") {
                        repo.getDeviceAddress(deviceWithHexInfo) shouldBe address
                    }
                    then("save this address in cache") {
                        coVerify(exactly = 1) {
                            cacheAddressSource.setLocationAddress(hexIndex, address)
                        }
                    }
                }
            }
        }
    }

})
