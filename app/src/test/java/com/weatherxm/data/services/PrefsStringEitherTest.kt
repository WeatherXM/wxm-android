package com.weatherxm.data.services

import android.content.SharedPreferences
import arrow.core.Either
import com.weatherxm.TestConfig.sharedPref
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.data.models.DataError
import com.weatherxm.data.models.Failure
import com.weatherxm.data.services.CacheService.Companion.KEY_INSTALLATION_ID
import com.weatherxm.data.services.CacheService.Companion.KEY_USERNAME
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.every
import io.mockk.verify

class PrefsStringEitherTest(
    private val cacheService: CacheService,
    private val prefEditor: SharedPreferences.Editor
) {
    private val installationId = "installationId"
    private val username = "username"
    private val otaKey = "otaKey"
    private val otaVersion = "1.0.0"
    private val widgetKey = "1.0.0"
    private val deviceId = "deviceId"

    @Suppress("LongParameterList")
    private fun BehaviorSpec.testStringEither(
        dataTitle: String,
        data: Any,
        mockFunction: () -> Any?,
        verifyFunction: () -> Any,
        getFunction: () -> Either<Failure, Any>,
        setFunction: () -> Any
    ): BehaviorSpec {
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
        return this
    }


    fun test(behaviorSpec: BehaviorSpec) {
        behaviorSpec.testStringEither(
            "Installation ID",
            installationId,
            { sharedPref.getString(KEY_INSTALLATION_ID, null) },
            { prefEditor.putString(KEY_INSTALLATION_ID, installationId) },
            { cacheService.getInstallationId() },
            { cacheService.setInstallationId(installationId) }
        )

        behaviorSpec.testStringEither(
            "Username",
            username,
            { sharedPref.getString(KEY_USERNAME, null) },
            { prefEditor.putString(KEY_USERNAME, username) },
            { cacheService.getUserUsername() },
            { cacheService.setUserUsername(username) }
        )

        behaviorSpec.testStringEither(
            "Last OTA version we showed a notification",
            otaVersion,
            { sharedPref.getString(otaKey, null) },
            { prefEditor.putString(otaKey, otaVersion) },
            { cacheService.getDeviceLastOtaVersion(otaKey) },
            { cacheService.setDeviceLastOtaVersion(otaKey, otaVersion) }
        )

        behaviorSpec.testStringEither(
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
    }

}
