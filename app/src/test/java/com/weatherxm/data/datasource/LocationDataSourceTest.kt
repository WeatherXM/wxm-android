package com.weatherxm.data.datasource

import android.content.Context.TELEPHONY_SERVICE
import android.telephony.TelephonyManager
import com.squareup.moshi.Moshi
import com.weatherxm.TestConfig.context
import com.weatherxm.data.models.CountryInfo
import com.weatherxm.data.models.Location
import com.weatherxm.data.services.CacheService
import com.weatherxm.ui.common.empty
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coJustRun
import io.mockk.every
import io.mockk.mockk
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.java.KoinJavaComponent.inject
import org.koin.test.KoinTest

class LocationDataSourceTest : KoinTest, BehaviorSpec({
    val cacheService = mockk<CacheService>()
    val moshi: Moshi by inject(Moshi::class.java)
    lateinit var datasource: LocationDataSourceImpl

    val telephonyManager = mockk<TelephonyManager>()
    val country = "GR"
    val location = Location(39.074208, 21.824312)
    val validCountriesInfo = listOf(CountryInfo("GR", "EU868", location))
    val otherCountriesInfo = listOf(CountryInfo("US", null, location))
    val invalidCountriesInfo = listOf(CountryInfo("GR", null, null))

    startKoin {
        modules(
            module {
                single<Moshi> {
                    Moshi.Builder().build()
                }
            }
        )
    }

    beforeSpec {
        datasource = LocationDataSourceImpl(context, moshi, cacheService)
        every { context.getSystemService(TELEPHONY_SERVICE) } returns telephonyManager
        every { telephonyManager.simCountryIso } returns String.empty()
        every { telephonyManager.networkCountryIso } returns String.empty()
        coJustRun { cacheService.setCountriesInfo(any()) }
    }

    context("Get user country") {
        When("sim country and network country are empty") {
            then("return null") {
                datasource.getUserCountry() shouldBe null
            }
        }
        When("sim country is empty and network country is NOT empty") {
            every { telephonyManager.networkCountryIso } returns country
            then("return the country") {
                datasource.getUserCountry() shouldBe country
            }
        }
        When("sim country is NOT empty and network country is empty") {
            every { telephonyManager.simCountryIso } returns country
            every { telephonyManager.networkCountryIso } returns ""
            then("return the country") {
                datasource.getUserCountry() shouldBe country
            }
        }
    }

    context("Get user's country location") {
        When("user's country is null") {
            every { telephonyManager.simCountryIso } returns ""
            every { telephonyManager.networkCountryIso } returns ""
            then("return null") {
                datasource.getUserCountryLocation() shouldBe null
            }
        }
        When("user's country is NOT null") {
            every { telephonyManager.simCountryIso } returns country
            and("cache is empty") {
                every { cacheService.getCountriesInfo() } returns emptyList()
                and("we use the countries_information.json") {
                    then("return the country's map center as found") {
                        datasource.getUserCountryLocation() shouldBe location
                    }
                }
            }
            and("cache is not empty and contains the user's country") {
                and("has the map center") {
                    every { cacheService.getCountriesInfo() } returns validCountriesInfo
                    then("returns the location (country's map center)") {
                        datasource.getUserCountryLocation() shouldBe location
                    }
                }
                and("does NOT have the map center") {
                    every { cacheService.getCountriesInfo() } returns invalidCountriesInfo
                    then("returns null") {
                        datasource.getUserCountryLocation() shouldBe null
                    }
                }
            }
            and("cache is not empty and does not contain the user's country") {
                every { cacheService.getCountriesInfo() } returns otherCountriesInfo
                then("returns null") {
                    datasource.getUserCountryLocation() shouldBe null
                }
            }
        }
    }

    afterSpec {
        stopKoin()
    }
})
