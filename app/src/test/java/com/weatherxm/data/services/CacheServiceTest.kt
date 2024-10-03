package com.weatherxm.data.services

import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import com.weatherxm.TestConfig.resources
import com.weatherxm.TestConfig.sharedPref
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.data.models.DataError
import com.weatherxm.data.network.AuthToken
import com.weatherxm.data.services.CacheService.Companion.KEY_ACCESS
import com.weatherxm.data.services.CacheService.Companion.KEY_CURRENT_WEATHER_WIDGET_IDS
import com.weatherxm.data.services.CacheService.Companion.KEY_REFRESH
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
    val widgetIdsSet = setOf("1", "2", "3")

    beforeSpec {
        every { encryptedPref.edit() } returns prefEditor
        every { sharedPref.edit() } returns prefEditor
        every { prefEditor.putString(any(), any()) } returns prefEditor
        every { prefEditor.putBoolean(any(), any()) } returns prefEditor
        every { prefEditor.putInt(any(), any()) } returns prefEditor
        every { prefEditor.putLong(any(), any()) } returns prefEditor
        every { prefEditor.putStringSet(any(), any()) } returns prefEditor
        every { prefEditor.remove(any()) } returns prefEditor
        every { prefEditor.clear() } returns prefEditor
        justRun { prefEditor.apply() }
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

    context("GET / SET the set of widget IDs") {
        When("GET the set of widget IDs") {
            and("The set is null") {
                every {
                    sharedPref.getStringSet(KEY_CURRENT_WEATHER_WIDGET_IDS, null)
                } returns null
                then("return CacheMissError") {
                    cacheService.getWidgetIds().leftOrNull()
                        .shouldBeTypeOf<DataError.CacheMissError>()
                }
            }
            and("The set is empty") {
                every {
                    sharedPref.getStringSet(KEY_CURRENT_WEATHER_WIDGET_IDS, null)
                } returns setOf()
                then("return CacheMissError") {
                    cacheService.getWidgetIds().leftOrNull()
                        .shouldBeTypeOf<DataError.CacheMissError>()
                }
            }
            and("The set is NOT null nor empty") {
                every {
                    sharedPref.getStringSet(KEY_CURRENT_WEATHER_WIDGET_IDS, null)
                } returns widgetIdsSet
                then("return the Installation ID") {
                    cacheService.getWidgetIds().isSuccess(widgetIdsSet.toList())
                }
            }
        }
        When("SET the set of widget IDs") {
            then("set them in the Shared Preferences") {
                cacheService.setWidgetIds(widgetIdsSet.toList())
                verify(exactly = 1) {
                    prefEditor.putStringSet(KEY_CURRENT_WEATHER_WIDGET_IDS, widgetIdsSet)
                }
            }
        }
    }

    PrefsSingleVarTest(cacheService, prefEditor).test(this)

    InMemoryTest(cacheService).test(this)

    PrefsStringEitherTest(cacheService, prefEditor).test(this)
})
