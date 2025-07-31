package com.weatherxm.data.datasource

import android.content.Context.TELEPHONY_SERVICE
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.telephony.TelephonyManager
import com.mapbox.geojson.Point
import com.mapbox.search.ReverseGeoOptions
import com.mapbox.search.SearchCallback
import com.mapbox.search.SearchEngine
import com.mapbox.search.common.AsyncOperationTask
import com.mapbox.search.common.SearchCancellationException
import com.mapbox.search.result.SearchResult
import com.squareup.moshi.Moshi
import com.weatherxm.TestConfig.cacheService
import com.weatherxm.TestConfig.context
import com.weatherxm.TestConfig.geocoder
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.data.datasource.ReverseGeocodingDataSourceImpl.Companion.SEARCH_LIMIT
import com.weatherxm.data.datasource.ReverseGeocodingDataSourceImpl.Companion.SEARCH_TYPES
import com.weatherxm.data.models.CancellationError
import com.weatherxm.data.models.CountryAndFrequencies
import com.weatherxm.data.models.CountryInfo
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.Frequency
import com.weatherxm.data.models.Location
import com.weatherxm.data.models.MapBoxError
import com.weatherxm.ui.common.empty
import com.weatherxm.util.AndroidBuildInfo
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.java.KoinJavaComponent.inject
import org.koin.test.KoinTest
import java.io.InputStream

class ReverseGeocodingDataSourceTest : KoinTest, BehaviorSpec({
    val searchEngine = mockk<SearchEngine>()
    val moshi: Moshi by inject(Moshi::class.java)
    lateinit var dataSource: ReverseGeocodingDataSourceImpl

    val location = Location.empty()
    val countryLocation = Location(39.074208, 21.824312)
    val countryName = "Greece"
    val countryCode = "GR"
    val mockedAddress = mockk<Address>().apply {
        every { this@apply.locality } returns ""
        every { this@apply.adminArea } returns ""
        every { this@apply.subAdminArea } returns ""
        every { this@apply.subAdminArea } returns ""
        every { this@apply.countryName } returns countryName
        every { this@apply.countryCode } returns countryCode
    }
    val point = mockk<Point>()
    val countriesAndFrequencies = CountryAndFrequencies(
        countryName,
        Frequency.EU868,
        listOf(
            Frequency.US915,
            Frequency.AU915,
            Frequency.CN470,
            Frequency.KR920,
            Frequency.IN865,
            Frequency.RU864,
            Frequency.AS923_1,
            Frequency.AS923_1B,
            Frequency.AS923_2,
            Frequency.AS923_3,
            Frequency.AS923_4
        )
    )

    val reverseGeoOptions =
        ReverseGeoOptions(center = point, limit = SEARCH_LIMIT, types = SEARCH_TYPES)
    val searchResultSlot = slot<SearchCallback>()
    val searchResult = mockk<SearchResult>()

    /**
     * Open the file under test/resources/countries_information.json as an InputStream
     * otherwise use a default ByteArrayInputStream as specified below
     */
    val countriesInformationFirstStream =
        javaClass.classLoader?.getResourceAsStream("countries_information.json")
    val countriesInformationSecondStream =
        javaClass.classLoader?.getResourceAsStream("countries_information.json")
    val countriesInformationThirdStream =
        javaClass.classLoader?.getResourceAsStream("countries_information.json")

    val telephonyManager = mockk<TelephonyManager>()
    val country = "GR"
    val validCountriesInfo = listOf(CountryInfo("GR", "EU868", countryLocation))
    val otherCountriesInfo = listOf(CountryInfo("US", null, countryLocation))
    val invalidCountriesInfo = listOf(CountryInfo("GR", null, null))

    startKoin {
        modules(
            module {
                single<Moshi> {
                    Moshi.Builder().build()
                }
                single<Geocoder> {
                    geocoder
                }
            }
        )
    }

    @Suppress("DEPRECATION")
    beforeSpec {
        mockkStatic(Geocoder::class)
        every { AndroidBuildInfo.sdkInt } returns Build.VERSION_CODES.TIRAMISU - 1
        dataSource = ReverseGeocodingDataSourceImpl(context, searchEngine, moshi, cacheService)
        every {
            geocoder.getFromLocation(any<Double>(), any<Double>(), 1)
        } returns listOf(mockedAddress)
        every { context.getSystemService(TELEPHONY_SERVICE) } returns telephonyManager
        every { telephonyManager.simCountryIso } returns String.empty()
        every { telephonyManager.networkCountryIso } returns String.empty()
        coJustRun { cacheService.setCountriesInfo(any()) }
    }

    context("Get the address of a Point") {
        When("Using the Network Source") {
            and("the response is a success") {
                and("the results returned are empty") {
                    coEvery {
                        searchEngine.search(reverseGeoOptions, capture(searchResultSlot))
                    }.answers {
                        searchResultSlot.captured.onResults(listOf(searchResult), mockk())
                        AsyncOperationTask.COMPLETED
                    }
                    then("return the first search result") {
                        dataSource.getAddressFromPoint(point).isSuccess(searchResult)
                    }
                }
                and("the results returned are NOT empty") {
                    coEvery {
                        searchEngine.search(reverseGeoOptions, capture(searchResultSlot))
                    }.answers {
                        searchResultSlot.captured.onResults(mutableListOf(), mockk())
                        AsyncOperationTask.COMPLETED
                    }
                    then("return GeocodingError") {
                        dataSource.getAddressFromPoint(point).leftOrNull()
                            .shouldBeTypeOf<MapBoxError.GeocodingError>()
                    }
                }
            }
            and("the response is a failure") {
                and("the exception is a SearchCancellationException") {
                    coEvery {
                        searchEngine.search(reverseGeoOptions, capture(searchResultSlot))
                    }.answers {
                        searchResultSlot.captured.onError(SearchCancellationException(""))
                        AsyncOperationTask.COMPLETED
                    }
                    then("return CancellationError") {
                        dataSource.getAddressFromPoint(point).leftOrNull()
                            .shouldBeTypeOf<CancellationError>()
                    }
                }
                and("the exception is NOT a SearchCancellationException") {
                    coEvery {
                        searchEngine.search(reverseGeoOptions, capture(searchResultSlot))
                    }.answers {
                        searchResultSlot.captured.onError(Exception())
                        AsyncOperationTask.COMPLETED
                    }
                    then("return GeocodingError") {
                        dataSource.getAddressFromPoint(point).leftOrNull()
                            .shouldBeTypeOf<MapBoxError.GeocodingError>()
                    }
                }
            }

        }
    }

    context("Get countries and frequencies using a location") {
        When("Using the Network Source") {
            and("get the country from the location is a failure") {
                every { Geocoder.isPresent() } returns false
                then("return CountryNotFound failure") {
                    dataSource.getCountryAndFrequencies(location).leftOrNull()
                        .shouldBeTypeOf<Failure.CountryNotFound>()
                }
            }
            and("get the country from the location is a success") {
                every { Geocoder.isPresent() } returns true
                every {
                    context.assets.open("countries_information.json")
                } returns countriesInformationFirstStream as InputStream
                and("getting the frequency of the country is a success") {
                    then("return the respective CountriesAndFrequencies") {
                        dataSource.getCountryAndFrequencies(location)
                            .isSuccess(countriesAndFrequencies)
                    }
                }
                and("getting the frequency of the country is a failure") {
                    every { mockedAddress.countryCode } returns "UNKNOWN_COUNTRY_CODE"
                    every {
                        context.assets.open("countries_information.json")
                    } returns countriesInformationSecondStream as InputStream
                    then("return CountryNotFound failure") {
                        dataSource.getCountryAndFrequencies(location).leftOrNull()
                            .shouldBeTypeOf<Failure.CountryNotFound>()
                    }
                }
            }
        }
    }

    context("Get user country") {
        When("sim country and network country are empty") {
            then("return null") {
                dataSource.getUserCountry() shouldBe null
            }
        }
        When("sim country is empty and network country is NOT empty") {
            every { telephonyManager.networkCountryIso } returns country
            then("return the country") {
                dataSource.getUserCountry() shouldBe country
            }
        }
        When("sim country is NOT empty and network country is empty") {
            every { telephonyManager.simCountryIso } returns country
            every { telephonyManager.networkCountryIso } returns ""
            then("return the country") {
                dataSource.getUserCountry() shouldBe country
            }
        }
    }

    context("Get user's country location") {
        When("user's country is null") {
            every { telephonyManager.simCountryIso } returns ""
            every { telephonyManager.networkCountryIso } returns ""
            then("return null") {
                dataSource.getUserCountryLocation() shouldBe null
            }
        }
        When("user's country is NOT null") {
            every { telephonyManager.simCountryIso } returns country
            and("cache is empty") {
                every { cacheService.getCountriesInfo() } returns emptyList()
                every {
                    context.assets.open("countries_information.json")
                } returns countriesInformationThirdStream as InputStream
                and("we use the countries_information.json") {
                    then("return the country's map center as found") {
                        dataSource.getUserCountryLocation() shouldBe countryLocation
                    }
                }
            }
            and("cache is not empty and contains the user's country") {
                and("has the map center") {
                    every { cacheService.getCountriesInfo() } returns validCountriesInfo
                    then("returns the location (country's map center)") {
                        dataSource.getUserCountryLocation() shouldBe countryLocation
                    }
                }
                and("does NOT have the map center") {
                    every { cacheService.getCountriesInfo() } returns invalidCountriesInfo
                    then("returns null") {
                        dataSource.getUserCountryLocation() shouldBe null
                    }
                }
            }
            and("cache is not empty and does not contain the user's country") {
                every { cacheService.getCountriesInfo() } returns otherCountriesInfo
                then("returns null") {
                    dataSource.getUserCountryLocation() shouldBe null
                }
            }
        }
    }

    afterSpec {
        stopKoin()
    }
})
