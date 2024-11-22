package com.weatherxm.data.services

import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import com.weatherxm.R
import com.weatherxm.TestConfig.resources
import com.weatherxm.TestConfig.sharedPref
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.data.models.DataError
import com.weatherxm.data.models.WeatherData
import com.weatherxm.data.network.AuthToken
import com.weatherxm.data.services.CacheService.Companion.KEY_ACCESS
import com.weatherxm.data.services.CacheService.Companion.KEY_CURRENT_WEATHER_WIDGET_IDS
import com.weatherxm.data.services.CacheService.Companion.KEY_DEVICES_FILTER
import com.weatherxm.data.services.CacheService.Companion.KEY_DEVICES_GROUP_BY
import com.weatherxm.data.services.CacheService.Companion.KEY_DEVICES_SORT
import com.weatherxm.data.services.CacheService.Companion.KEY_REFRESH
import com.weatherxm.data.services.CacheService.Companion.KEY_TEMPERATURE
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
    val sortOption = "sortOption"
    val filterOption = "filterOption"
    val groupByOption = "groupByOption"
    val deviceId = "deviceId"
    val celsiusUnit = "°C"
    val weatherData = listOf<WeatherData>(mockk())

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
        every { sharedPref.getStringSet("current_weather_widget_ids", setOf()) } returns setOf()
        justRun { okHttpCache.evictAll() }
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

    context("GET / SET the device sort, filter and group options") {
        When("GET the device sort, filter and group options") {
            and("The sort order, filter and group options are null") {
                every { sharedPref.getString(KEY_DEVICES_SORT, null) } returns null
                every { sharedPref.getString(KEY_DEVICES_FILTER, null) } returns null
                every { sharedPref.getString(KEY_DEVICES_GROUP_BY, null) } returns null
                then("return an empty list") {
                    cacheService.getDevicesSortFilterOptions() shouldBe emptyList()
                }
            }
            and("The sort order, filter and group options are NOT null") {
                every { sharedPref.getString(KEY_DEVICES_SORT, null) } returns sortOption
                every { sharedPref.getString(KEY_DEVICES_FILTER, null) } returns filterOption
                every { sharedPref.getString(KEY_DEVICES_GROUP_BY, null) } returns groupByOption
                then("return the list with the options") {
                    cacheService.getDevicesSortFilterOptions() shouldBe listOf(
                        sortOption, filterOption, groupByOption
                    )
                }
            }
            and("If one of them is null (e.g. sort order is null)") {
                every { sharedPref.getString(KEY_DEVICES_SORT, null) } returns null
                then("return the list of non-null options") {
                    cacheService.getDevicesSortFilterOptions() shouldBe listOf(
                        filterOption, groupByOption
                    )
                }
            }
        }
        When("SET the device sort, filter and group options") {
            then("set these options in the Shared Preferences") {
                cacheService.setDevicesSortFilterOptions(sortOption, filterOption, groupByOption)
                verify(exactly = 1) { prefEditor.putString(KEY_DEVICES_SORT, sortOption) }
                verify(exactly = 1) { prefEditor.putString(KEY_DEVICES_FILTER, filterOption) }
                verify(exactly = 1) { prefEditor.putString(KEY_DEVICES_GROUP_BY, groupByOption) }
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

    context("GET / SET the device's forecast") {
        given("a forecast for a device") {
            then("set it as an in-memory variable") {
                cacheService.setForecast(deviceId, weatherData)
            }
            and("GET this forecast") {
                and("the forecast is valid and NOT expired") {
                    then("return the forecast") {
                        cacheService.getForecast(deviceId).isSuccess(weatherData)
                    }
                }
            }
            and("clear the forecast") {
                cacheService.clearForecast()
                then("return CacheMissError") {
                    cacheService.getForecast(deviceId).leftOrNull()
                        .shouldBeTypeOf<DataError.CacheMissError>()
                }
            }
        }
    }

    context("Get a preferred unit") {
        given("the key for the preferred unit and the default fallback") {
            and("there is no such key in the Shared Preferences") {
                every {
                    sharedPref.getString(
                        resources.getString(KEY_TEMPERATURE),
                        resources.getString(R.string.temperature_celsius)
                    )
                } returns null
                then("return the default fallback") {
                    cacheService.getPreferredUnit(
                        KEY_TEMPERATURE,
                        R.string.temperature_celsius
                    ) shouldBe celsiusUnit
                }
            }
            and("there is a such key in the Shared Preferences") {
                every {
                    sharedPref.getString(
                        resources.getString(KEY_TEMPERATURE),
                        resources.getString(R.string.temperature_celsius)
                    )
                } returns celsiusUnit
                then("return the selected unit or the default fallback") {
                    cacheService.getPreferredUnit(
                        KEY_TEMPERATURE,
                        R.string.temperature_celsius
                    ) shouldBe celsiusUnit
                }
            }
        }
    }

    /**
     * Some unit tests that can be grouped together for better code readability
     */
    PrefsSingleVarTest(cacheService, prefEditor).test(this)
    InMemoryTest(cacheService).test(this)
    PrefsStringEitherTest(cacheService, prefEditor).test(this)

    context("Clear the cache") {
        When("we want to clear everything") {
            then("call the respective clearAll function") {
                cacheService.clearAll()
                cacheService.isCacheEmpty() shouldBe true
            }
        }
    }
})
