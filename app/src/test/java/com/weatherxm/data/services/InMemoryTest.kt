package com.weatherxm.data.services

import arrow.core.Either
import com.mapbox.search.result.SearchSuggestion
import com.weatherxm.TestConfig.sharedPref
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.data.models.CountryInfo
import com.weatherxm.data.models.DataError
import com.weatherxm.data.models.Device
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.Location
import com.weatherxm.data.models.User
import com.weatherxm.data.services.CacheService.Companion.KEY_DEVICES_FAVORITE
import com.weatherxm.data.services.CacheService.Companion.KEY_DEVICES_OWN
import com.weatherxm.data.services.CacheService.Companion.KEY_HAS_WALLET
import com.weatherxm.data.services.CacheService.Companion.KEY_USER_ID
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.every
import io.mockk.mockk
import net.gotev.uploadservice.protocols.multipart.MultipartUploadRequest

class InMemoryTest(private val cacheService: CacheService) {
    private val walletAddress = "walletAddress"
    private val user = User("id", "email@email.com", null, null, null, null)
    private val searchSuggestion = mockk<SearchSuggestion>().apply {
        every { id } returns "searchSuggestionId"
    }
    private val searchSuggestions = listOf(searchSuggestion)
    private val location = Location.empty()
    private val query = "query"
    private val deviceId = "deviceId"
    private val uploadId = "uploadId"
    private val devices= listOf(Device.empty())
    private val deviceIds = listOf(deviceId)
    private val countriesInfo = listOf<CountryInfo>(mockk())
    private val uploadIds = listOf(uploadId)
    private val uploadIdRequest = mockk<MultipartUploadRequest>()

    private fun BehaviorSpec.testInMemoryEither(
        dataTitle: String,
        data: Any,
        getFunction: () -> Either<Failure, Any?>,
        setFunction: () -> Any
    ): BehaviorSpec {
        context("GET / SET $dataTitle") {
            When("GET the $dataTitle") {
                and("The $dataTitle is null") {
                    then("return CacheMissError") {
                        getFunction().leftOrNull().shouldBeTypeOf<DataError.CacheMissError>()
                    }
                }
                and("The $dataTitle is NOT null") {
                    setFunction()
                    then("return the $dataTitle") {
                        getFunction().isSuccess(data)
                    }
                }
            }
        }
        return this
    }

    private fun BehaviorSpec.testInMemorySingleVar(
        dataTitle: String,
        data: Any,
        getFunction: () -> Any,
        setFunction: () -> Any
    ): BehaviorSpec {
        context("GET / SET $dataTitle") {
            setFunction()
            When("GET the $dataTitle") {
                then("return the $dataTitle") {
                    getFunction() shouldBe data
                }
            }
        }
        return this
    }

    @Suppress("LongMethod")
    fun test(behaviorSpec: BehaviorSpec) {
        behaviorSpec.testInMemoryEither(
            "Wallet Address",
            walletAddress,
            { cacheService.getWalletAddress() },
            { cacheService.setWalletAddress(walletAddress) }
        ).apply {
            given("the wallet address has been set") {
                every { sharedPref.getBoolean(KEY_HAS_WALLET, false) } returns true
                then("return true") {
                    cacheService.hasWallet() shouldBe true
                }
            }
        }

        behaviorSpec.testInMemoryEither(
            "User",
            user,
            { cacheService.getUser() },
            { cacheService.setUser(user) }
        ).apply {
            given("that the user has been set") {
                every { sharedPref.getString(KEY_USER_ID, null) } returns user.id
                then("return true") {
                    cacheService.getUserId() shouldBe user.id
                }
            }
        }

        behaviorSpec.testInMemoryEither(
            "Search Suggestions",
            searchSuggestions,
            { cacheService.getSearchSuggestions(query) },
            { cacheService.setSearchSuggestions(query, searchSuggestions) }
        )

        behaviorSpec.testInMemoryEither(
            "Location of Search Suggestion",
            location,
            { cacheService.getSuggestionLocation(searchSuggestion) },
            { cacheService.setSuggestionLocation(searchSuggestion, location) }
        )

        behaviorSpec.testInMemorySingleVar(
            "IDs of the followed devices",
            deviceIds,
            { cacheService.getFollowedDevicesIds() },
            { cacheService.setFollowedDevicesIds(deviceIds) }
        ).apply {
            given("that the user has some favorite devices") {
                every { sharedPref.getInt(KEY_DEVICES_FAVORITE, 0) } returns deviceIds.size
                then("return the number of owned devices") {
                    cacheService.getDevicesFavorite() shouldBe deviceIds.size
                }
            }
        }

        behaviorSpec.testInMemorySingleVar(
            "Device's photo upload IDs",
            uploadIds,
            { cacheService.getDevicePhotoUploadIds(deviceId) },
            { cacheService.addDevicePhotoUploadId(deviceId, uploadId) }
        ).apply {
            given("one more uploadId") {
                then("add it") {
                    cacheService.addDevicePhotoUploadId(deviceId, "uploadId2")
                }
                and("remove the first one") {
                    cacheService.removeDevicePhotoUploadId(deviceId, uploadId)
                    then("return the second one") {
                        cacheService.getDevicePhotoUploadIds(deviceId) shouldBe listOf("uploadId2")
                    }
                }
            }
        }

        behaviorSpec.testInMemorySingleVar(
            "Upload ID's Multipart Request",
            uploadIdRequest,
            { cacheService.getUploadIdRequest(uploadId)!! },
            { cacheService.setUploadIdRequest(uploadId, uploadIdRequest) }
        ).apply {
            given("an action to remove it") {
                cacheService.removeUploadIdRequest(uploadId)
                then("null should be returned") {
                    cacheService.getUploadIdRequest(uploadId) shouldBe null
                }
            }
        }

        behaviorSpec.testInMemorySingleVar(
            "IDs of the user devices",
            devices,
            { cacheService.getUserDevices() },
            { cacheService.setUserDevices(devices) }
        ).apply {
            given("that the user has some owned devices") {
                every { sharedPref.getInt(KEY_DEVICES_OWN, 0) } returns devices.size
                then("return the number of owned devices") {
                    cacheService.getDevicesOwn() shouldBe devices.size
                }
            }
        }

        behaviorSpec.testInMemorySingleVar(
            "countries information",
            countriesInfo,
            { cacheService.getCountriesInfo() },
            { cacheService.setCountriesInfo(countriesInfo) }
        )
    }

}
