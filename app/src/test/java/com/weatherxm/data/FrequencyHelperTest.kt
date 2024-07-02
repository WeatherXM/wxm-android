package com.weatherxm.data

import android.content.Context
import com.squareup.moshi.Moshi
import com.weatherxm.data.Frequency
import com.weatherxm.data.adapters.LocalDateJsonAdapter
import com.weatherxm.data.adapters.LocalDateTimeJsonAdapter
import com.weatherxm.data.adapters.ZonedDateTimeJsonAdapter
import com.weatherxm.data.countryToFrequency
import com.weatherxm.data.frequencyToHeliumBleBandValue
import com.weatherxm.data.otherFrequencies
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
            every {
                context.assets.open("countries_information.json")
            } returns ByteArrayInputStream(
                ("[{\"code\": \"GR\"," +
                    "\"helium_frequency\": \"EU868\"," +
                    "\"map_center\": {\"lat\": 39.074208,\"lon\": 21.824312}}]"
                    ).toByteArray()
            )
            then("return the correct frequency (EU868)") {
                countryToFrequency(context, "GR", moshi) shouldBe Frequency.EU868
            }
        }
    }

    context("Mapping of Frequency to Helium Band Value for BLE") {
        given("a frequency value") {
            When("EU868") {
                then("Band Value = 5") {
                    frequencyToHeliumBleBandValue(Frequency.EU868) shouldBe 5
                }
            }
            When("US915") {
                then("Band Value = 8") {
                    frequencyToHeliumBleBandValue(Frequency.US915) shouldBe 8
                }
            }
            When("AU915") {
                then("Band Value = 1") {
                    frequencyToHeliumBleBandValue(Frequency.AU915) shouldBe 1
                }
            }
            When("CN470") {
                then("Band Value = 2") {
                    frequencyToHeliumBleBandValue(Frequency.CN470) shouldBe 2
                }
            }
            When("KR920") {
                then("Band Value = 6") {
                    frequencyToHeliumBleBandValue(Frequency.KR920) shouldBe 6
                }
            }
            When("IN865") {
                then("Band Value = 7") {
                    frequencyToHeliumBleBandValue(Frequency.IN865) shouldBe 7
                }
            }
            When("RU864") {
                then("Band Value = 9") {
                    frequencyToHeliumBleBandValue(Frequency.RU864) shouldBe 9
                }
            }
            When("AS923_1") {
                then("Band Value = 10") {
                    frequencyToHeliumBleBandValue(Frequency.AS923_1) shouldBe 10
                }
            }
            When("AS923_1B") {
                then("Band Value = 14") {
                    frequencyToHeliumBleBandValue(Frequency.AS923_1B) shouldBe 14
                }
            }
            When("AS923_2") {
                then("Band Value = 11") {
                    frequencyToHeliumBleBandValue(Frequency.AS923_2) shouldBe 11
                }
            }
            When("AS923_3") {
                then("Band Value = 12") {
                    frequencyToHeliumBleBandValue(Frequency.AS923_3) shouldBe 12
                }
            }
            When("AS923_4") {
                then("Band Value = 13") {
                    frequencyToHeliumBleBandValue(Frequency.AS923_4) shouldBe 13
                }
            }
        }
    }

    afterSpec {
        stopKoin()
    }
})
