package com.weatherxm.data.services

import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import arrow.core.Either
import com.mapbox.search.result.SearchSuggestion
import com.weatherxm.TestConfig.resources
import com.weatherxm.TestConfig.sharedPref
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.data.models.CountryInfo
import com.weatherxm.data.models.DataError
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.Location
import com.weatherxm.data.models.User
import com.weatherxm.data.network.AuthToken
import com.weatherxm.data.services.CacheService.Companion.KEY_ACCESS
import com.weatherxm.data.services.CacheService.Companion.KEY_ANALYTICS_OPT_IN_OR_OUT_TIMESTAMP
import com.weatherxm.data.services.CacheService.Companion.KEY_DEVICES_OWN
import com.weatherxm.data.services.CacheService.Companion.KEY_DISMISSED_INFO_BANNER_ID
import com.weatherxm.data.services.CacheService.Companion.KEY_DISMISSED_SURVEY_ID
import com.weatherxm.data.services.CacheService.Companion.KEY_HAS_WALLET
import com.weatherxm.data.services.CacheService.Companion.KEY_INSTALLATION_ID
import com.weatherxm.data.services.CacheService.Companion.KEY_LAST_REMINDED_VERSION
import com.weatherxm.data.services.CacheService.Companion.KEY_REFRESH
import com.weatherxm.data.services.CacheService.Companion.KEY_USERNAME
import com.weatherxm.data.services.CacheService.Companion.KEY_USER_ID
import com.weatherxm.data.services.CacheService.Companion.KEY_WALLET_WARNING_DISMISSED_TIMESTAMP
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import okhttp3.Cache
import org.koin.test.KoinTest

class CacheServiceTest : KoinTest, BehaviorSpec({
    val encryptedPref = mockk<EncryptedSharedPreferences>()
    val okHttpCache = mockk<Cache>()
    val cacheService = CacheService(sharedPref, encryptedPref, okHttpCache, resources)

    val prefEditor = mockk<SharedPreferences.Editor>()
    val authToken = AuthToken("access", "refresh")
    val installationId = "installationId"
    val lastRemindedVersion = 0
    val timestamp = System.currentTimeMillis()
    val hexIndex = "hexIndex"
    val address = "address"
    val username = "username"
    val walletAddress = "walletAddress"
    val user = User("id", "email@email.com", null, null, null, null)
    val searchSuggestion = mockk<SearchSuggestion>().apply {
        every { id } returns "searchSuggestionId"
    }
    val searchSuggestions = listOf(searchSuggestion)
    val location = Location(0.0, 0.0)
    val query = "query"
    val otaKey = "otaKey"
    val otaVersion = "1.0.0"
    val widgetKey = "1.0.0"
    val deviceId = "deviceId"
    val deviceIds = listOf(deviceId)
    val surveyId = "surveyId"
    val infoBannerId = "infoBannerId"
    val countriesInfo = listOf<CountryInfo>(mockk())

    beforeSpec {
        every { encryptedPref.edit() } returns prefEditor
        every { sharedPref.edit() } returns prefEditor
        every { prefEditor.putString(any(), any()) } returns prefEditor
        every { prefEditor.putBoolean(any(), any()) } returns prefEditor
        every { prefEditor.putInt(any(), any()) } returns prefEditor
        every { prefEditor.putLong(any(), any()) } returns prefEditor
        every { prefEditor.remove(any()) } returns prefEditor
        every { prefEditor.clear() } returns prefEditor
        justRun { prefEditor.apply() }
    }

    fun BehaviorSpec.testStringEither(
        dataTitle: String,
        data: Any,
        mockFunction: () -> Any?,
        verifyFunction: () -> Any,
        getFunction: () -> Either<Failure, Any>,
        setFunction: () -> Any
    ) {
        context("GET / SET $dataTitle") {
            When("GET the $dataTitle") {
                and("The $dataTitle is null") {
                    every { mockFunction() } returns null
                    then("return CacheMissError") {
                        getFunction().leftOrNull().shouldBeTypeOf<DataError.CacheMissError>()
                    }
                }
                and("The $dataTitle is empty") {
                    every { mockFunction() } returns ""
                    then("return CacheMissError") {
                        getFunction().leftOrNull().shouldBeTypeOf<DataError.CacheMissError>()
                    }
                }
                and("The $dataTitle is NOT null nor empty") {
                    every { mockFunction() } returns data
                    then("return the Installation ID") {
                        getFunction().isSuccess(data)
                    }
                }
            }
            When("SET the $dataTitle") {
                then("set the $dataTitle in the Shared Preferences") {
                    setFunction()
                    verify(exactly = 1) { verifyFunction() }
                }
            }
        }
    }

    fun BehaviorSpec.testGetSetSingleVar(
        dataTitle: String,
        data: Any,
        mockFunction: () -> Any?,
        verifyFunction: () -> Any,
        getFunction: () -> Any?,
        setFunction: () -> Any
    ) {
        context("GET / SET $dataTitle") {
            When("GET the $dataTitle") {
                then("return the $dataTitle") {
                    every { mockFunction() } returns data
                    getFunction() shouldBe data
                }
            }
            When("SET the $dataTitle") {
                then("set the $dataTitle in the Shared Preferences") {
                    setFunction()
                    verify(exactly = 1) { verifyFunction() }
                }
            }
        }
    }

    fun BehaviorSpec.testInMemoryEither(
        dataTitle: String,
        data: Any,
        getFunction: () -> Either<Failure, Any?>,
        setFunction: () -> Any
    ) {
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
    }

    fun BehaviorSpec.testInMemorySingleVar(
        dataTitle: String,
        data: Any,
        getFunction: () -> Any,
        setFunction: () -> Any
    ) {
        context("GET / SET $dataTitle") {
            setFunction()
            When("GET the $dataTitle") {
                then("return the $dataTitle") {
                    getFunction() shouldBe data
                }
            }
        }
    }

    context("Get / Set Auth Token") {
        When("GET the Auth Token") {
            and("The access token and the refresh token are null") {
                every { encryptedPref.getString(KEY_ACCESS, null) } returns null
                every { encryptedPref.getString(KEY_REFRESH, null) } returns null
                then("return CacheMissError") {
                    cacheService.getAuthToken().leftOrNull()
                        .shouldBeTypeOf<DataError.CacheMissError>()
                }
            }
            and("The access token is null but the refresh token is NOT null") {
                every { encryptedPref.getString(KEY_REFRESH, null) } returns authToken.refresh
                then("return the AuthToken") {
                    cacheService.getAuthToken().onRight {
                        it.access shouldBe null
                        it.refresh shouldBe authToken.refresh
                    }
                }
            }
            and("The access token is NOT null but the refresh token is null") {
                every { encryptedPref.getString(KEY_ACCESS, null) } returns authToken.access
                every { encryptedPref.getString(KEY_REFRESH, null) } returns null
                then("return the AuthToken") {
                    cacheService.getAuthToken().onRight {
                        it.access shouldBe authToken.access
                        it.refresh shouldBe null
                    }
                }
            }
            and("The access token and the refresh token are NOT null") {
                every { encryptedPref.getString(KEY_REFRESH, null) } returns authToken.refresh
                then("return the AuthToken") {
                    cacheService.getAuthToken().onRight {
                        it.access shouldBe authToken.access
                        it.refresh shouldBe authToken.refresh
                    }
                }
            }
        }
        When("SET the Auth Token") {
            then("set the Auth Token in the Encrypted Shared Preferences") {
                cacheService.setAuthToken(authToken)
                verify(exactly = 1) { prefEditor.putString(KEY_ACCESS, authToken.access) }
                verify(exactly = 1) { prefEditor.putString(KEY_REFRESH, authToken.refresh) }
            }
        }
    }

    testStringEither(
        "Installation ID",
        installationId,
        { sharedPref.getString(KEY_INSTALLATION_ID, null) },
        { prefEditor.putString(KEY_INSTALLATION_ID, installationId) },
        { cacheService.getInstallationId() },
        { cacheService.setInstallationId(installationId) }
    )

    context("GET / SET Last Reminded Version") {
        When("GET the Last Reminded Version") {
            then("return the Last Reminded Version") {
                every {
                    sharedPref.getInt(KEY_LAST_REMINDED_VERSION, 0)
                } returns lastRemindedVersion
                cacheService.getLastRemindedVersion() shouldBe lastRemindedVersion
            }
        }
        When("SET the Last Reminded Version") {
            then("set the Last Reminded Version in the Shared Preferences") {
                cacheService.setLastRemindedVersion(lastRemindedVersion)
                verify(exactly = 1) {
                    prefEditor.putInt(KEY_LAST_REMINDED_VERSION, lastRemindedVersion)
                }
            }
        }
    }

    testGetSetSingleVar(
        "Analytics Decision Timestamp",
        timestamp,
        { sharedPref.getLong(KEY_ANALYTICS_OPT_IN_OR_OUT_TIMESTAMP, 0) },
        { prefEditor.putLong(KEY_ANALYTICS_OPT_IN_OR_OUT_TIMESTAMP, timestamp) },
        { cacheService.getAnalyticsDecisionTimestamp() },
        { cacheService.setAnalyticsDecisionTimestamp(timestamp) }
    )

    testGetSetSingleVar(
        "Analytics Enabled flag",
        true,
        { sharedPref.getBoolean("google_analytics", false) },
        { prefEditor.putBoolean("google_analytics", false) },
        { cacheService.getAnalyticsEnabled() },
        { cacheService.setAnalyticsEnabled(false) }
    )

    testStringEither(
        "Address of the Location",
        address,
        { sharedPref.getString(hexIndex, null) },
        { prefEditor.putString(hexIndex, address) },
        { cacheService.getLocationAddress(hexIndex) },
        { cacheService.setLocationAddress(hexIndex, address) }
    )

    testStringEither(
        "Username",
        username,
        { sharedPref.getString(KEY_USERNAME, null) },
        { prefEditor.putString(KEY_USERNAME, username) },
        { cacheService.getUserUsername() },
        { cacheService.setUserUsername(username) }
    )

    testInMemoryEither(
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

    testInMemoryEither(
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

    testInMemoryEither(
        "Search Suggestions",
        searchSuggestions,
        { cacheService.getSearchSuggestions(query) },
        { cacheService.setSearchSuggestions(query, searchSuggestions) }
    )

    testInMemoryEither(
        "Location of Search Suggestion",
        location,
        { cacheService.getSuggestionLocation(searchSuggestion) },
        { cacheService.setSuggestionLocation(searchSuggestion, location) }
    )

    testGetSetSingleVar(
        "Wallet Warning Dismiss Timestamp",
        timestamp,
        { sharedPref.getLong(KEY_WALLET_WARNING_DISMISSED_TIMESTAMP, 0) },
        { prefEditor.putLong(KEY_WALLET_WARNING_DISMISSED_TIMESTAMP, timestamp) },
        { cacheService.getWalletWarningDismissTimestamp() },
        { cacheService.setWalletWarningDismissTimestamp(timestamp) }
    )

    testStringEither(
        "Last OTA version we showed a notification",
        otaVersion,
        { sharedPref.getString(otaKey, null) },
        { prefEditor.putString(otaKey, otaVersion) },
        { cacheService.getDeviceLastOtaVersion(otaKey) },
        { cacheService.setDeviceLastOtaVersion(otaKey, otaVersion) }
    )

    testGetSetSingleVar(
        "Timestamp of the Last OTA version's we showed a notification",
        timestamp,
        { sharedPref.getLong(otaKey, 0) },
        { prefEditor.putLong(otaKey, timestamp) },
        { cacheService.getDeviceLastOtaTimestamp(otaKey) },
        { cacheService.setDeviceLastOtaTimestamp(otaKey, timestamp) }
    )

    testStringEither(
        "associated Device ID of the widget",
        deviceId,
        { sharedPref.getString(widgetKey, null) },
        { prefEditor.putString(widgetKey, deviceId) },
        { cacheService.getWidgetDevice(widgetKey) },
        { cacheService.setWidgetDevice(widgetKey, deviceId) }
    ).apply {
        given("a request to remove the associated device of the widget") {
            then("remove the respective entry in the Shared Preferences") {
                cacheService.removeDeviceOfWidget(widgetKey)
                verify(exactly = 1) { prefEditor.remove(widgetKey) }
            }
        }
    }

    testInMemorySingleVar(
        "IDs of the followed devices",
        deviceIds,
        { cacheService.getFollowedDevicesIds() },
        { cacheService.setFollowedDevicesIds(deviceIds) }
    )

    testInMemorySingleVar(
        "IDs of the user devices",
        deviceIds,
        { cacheService.getUserDevicesIds() },
        { cacheService.setUserDevicesIds(deviceIds) }
    ).apply {
        given("that the user has some owned devices") {
            every { sharedPref.getInt(KEY_DEVICES_OWN, 0) } returns deviceIds.size
            then("return the number of owned devices") {
                cacheService.getDevicesOwn() shouldBe deviceIds.size
            }
        }
    }

    testGetSetSingleVar(
        "ID of the last dismissed survey",
        surveyId,
        { sharedPref.getString(KEY_DISMISSED_SURVEY_ID, null) },
        { prefEditor.putString(KEY_DISMISSED_SURVEY_ID, surveyId) },
        { cacheService.getLastDismissedSurveyId() },
        { cacheService.setLastDismissedSurveyId(surveyId) }
    )

    testGetSetSingleVar(
        "ID of the last dismissed info banner",
        infoBannerId,
        { sharedPref.getString(KEY_DISMISSED_INFO_BANNER_ID, null) },
        { prefEditor.putString(KEY_DISMISSED_INFO_BANNER_ID, infoBannerId) },
        { cacheService.getLastDismissedInfoBannerId() },
        { cacheService.setLastDismissedInfoBannerId(infoBannerId) }
    )

    testInMemorySingleVar(
        "countries information",
        countriesInfo,
        { cacheService.getCountriesInfo() },
        { cacheService.setCountriesInfo(countriesInfo) }
    )

})
