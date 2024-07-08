package com.weatherxm.data

import android.content.Context
import com.squareup.moshi.Moshi
import com.weatherxm.data.adapters.LocalDateJsonAdapter
import com.weatherxm.data.adapters.LocalDateTimeJsonAdapter
import com.weatherxm.data.adapters.ZonedDateTimeJsonAdapter
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.java.KoinJavaComponent.inject
import org.koin.test.KoinTest
import java.io.ByteArrayInputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime

class FrequencyHelperTest : KoinTest, BehaviorSpec({
    val moshi: Moshi by inject(Moshi::class.java)

    startKoin {
        modules(
            module {
                single<Moshi> {
                    Moshi.Builder()
                        .add(ZonedDateTime::class.java, ZonedDateTimeJsonAdapter())
                        .add(LocalDateTime::class.java, LocalDateTimeJsonAdapter())
                        .add(LocalDate::class.java, LocalDateJsonAdapter())
                        .build()
                }
            }
        )
    }

    context("Get all frequencies but one") {
        given("the frequency that we want to be excluded") {
            then("return the rest of the frequencies") {
                with(otherFrequencies(Frequency.EU868)) {
                    this shouldNotContain Frequency.EU868
                    this.size shouldBe Frequency.entries.size - 1
                }
            }
        }
    }

    context("Mapping of Country to Frequency") {
        given("a country code (`GR`)") {
            val context = mockk<Context>()

            /**
             * Open the file under test/resources/countries_information.json as an InputStream
             * otherwise use a default ByteArrayInputStream as specified below
             */
            val mappingInputStream =
                this.javaClass.classLoader?.getResourceAsStream("countries_information.json")
                    ?: ByteArrayInputStream(
                        ("[{\"code\": \"GR\"," +
                            "\"helium_frequency\": \"EU868\"," +
                            "\"map_center\": {\"lat\": 39.074208,\"lon\": 21.824312}}]"
                            ).toByteArray()
                    )

            every { context.assets.open("countries_information.json") } returns mappingInputStream
            then("return the correct frequency (EU868)") {
                countryToFrequency(context, "GR", moshi) shouldBe Frequency.EU868
            }
        }
    }

    context("Mapping of Frequency to Helium Band Value for BLE") {
        val expectedResults = mapOf(
            "EU868" to 5,
            "US915" to 8,
            "AU915" to 1,
            "CN470" to 2,
            "KR920" to 6,
            "IN865" to 7,
            "RU864" to 9,
            "AS923_1" to 10,
            "AS923_1B" to 14,
            "AS923_2" to 11,
            "AS923_3" to 12,
            "AS923_4" to 13
        )
        given("a frequency value") {
            Frequency.entries.forEach {
                When(it.name) {
                    then("Band Value = ${expectedResults[it.name]}") {
                        frequencyToHeliumBleBandValue(it) shouldBe expectedResults[it.name]
                    }
                }
            }
        }
    }

    afterSpec {
        stopKoin()
    }
})
