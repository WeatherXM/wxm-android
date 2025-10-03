package com.weatherxm.data.repository

import com.weatherxm.TestConfig.failure
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isError
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.data.datasource.CacheDeviceDataSource
import com.weatherxm.data.datasource.CacheFollowDataSource
import com.weatherxm.data.datasource.NetworkDeviceDataSource
import com.weatherxm.data.models.Bundle
import com.weatherxm.data.models.Device
import com.weatherxm.data.models.DeviceInfo
import com.weatherxm.data.models.Location
import com.weatherxm.data.models.Relation
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk

class DeviceRepositoryTest : BehaviorSpec({
    lateinit var networkDeviceSource: NetworkDeviceDataSource
    lateinit var cacheDeviceSource: CacheDeviceDataSource
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
        Bundle(
            name = "m5",
            null,
            null,
            null,
            null,
            null
        ),
        null,
        null,
        null,
        null,
        Relation.owned,
        null,
        null,
        null
    )
    val ownedDevice2 = Device(
        "ownedId2",
        "",
        null,
        null,
        null,
        Bundle(
            name = "m5",
            null,
            null,
            null,
            null,
            null
        ),
        null,
        null,
        null,
        null,
        Relation.owned,
        null,
        null,
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
        null,
        null,
        null
    )
    val devices = mutableListOf(ownedDevice, ownedDevice2, followedDevice)
    val serialNumber = "serialNumber"
    val location = Location.empty()
    val deviceInfo = mockk<DeviceInfo>()
    val friendlyName = "My Weather Station"

    beforeContainer {
        networkDeviceSource = mockk()
        cacheDeviceSource = mockk()
        cacheFollowSource = mockk()
        repo = DeviceRepositoryImpl(
            networkDeviceSource,
            cacheDeviceSource,
            cacheFollowSource
        )
        coJustRun { cacheDeviceSource.setUserDevices(any()) }
        coJustRun { cacheFollowSource.setFollowedDevicesIds(any()) }
        coEvery { cacheDeviceSource.getUserDevicesFromCache() } returns emptyList()
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
                    coVerify(exactly = 1) {
                        cacheDeviceSource.setUserDevices(listOf(ownedDevice, ownedDevice2))
                    }
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
                    coVerify(exactly = 1) { cacheDeviceSource.getUserDevicesFromCache() }
                    coVerify(exactly = 1) { cacheDeviceSource.setUserDevices(listOf(ownedDevice)) }
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
                coEvery {
                    cacheDeviceSource.getUserDevicesFromCache()
                } returns listOf(ownedDevice, ownedDevice2)
                then("return that success") {
                    repo.removeDevice(serialNumber, ownedId).isSuccess(Unit)
                }
                then("remove this device from the rest owned devices in cache") {
                    coVerify(exactly = 1) { cacheDeviceSource.getUserDevicesFromCache() }
                    coVerify(exactly = 1) { cacheDeviceSource.setUserDevices(listOf(ownedDevice2)) }
                }
            }
        }
    }

    context("GET user devices IDs") {
        given("The cache that contains the user devices IDs") {
            coEvery { cacheDeviceSource.getUserDevicesFromCache() } returns listOf(ownedDevice)
            then("return the IDs") {
                repo.getUserDevicesIds() shouldBe listOf(ownedDevice.id)
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
                    coMockEitherRight(
                        { networkDeviceSource.clearFriendlyName(serialNumber) },
                        Unit
                    )
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
})
