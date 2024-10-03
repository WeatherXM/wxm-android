package com.weatherxm.util

import android.location.Address
import android.location.Geocoder
import android.os.Build
import arrow.core.Either
import com.weatherxm.TestConfig.geocoder
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.TestUtils.setStaticFieldViaReflection
import com.weatherxm.data.models.Failure
import com.weatherxm.util.GeocoderCompat.getFromLocation
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class GeocoderCompatTest : BehaviorSpec({
    val address = mockk<Address>()

    beforeSpec {
        startKoin {
            modules(
                module {
                    single<Geocoder> {
                        geocoder
                    }
                }
            )
        }

    }

    @Suppress("DEPRECATION")
    context("Get Address from Location using Geocoder") {
        When("Geocoder is NOT available") {
            every { Geocoder.isPresent() } returns false
            then("return a NoGeocoderError") {
                getFromLocation(0.0, 0.0) shouldBe
                    Either.Left(Failure.GeocoderError.NoGeocoderError)
            }
        }
        When("Geocoder is available") {
            every { Geocoder.isPresent() } returns true
            When("Build.VERSION.SDK_INT < TIRAMISU") {
                setStaticFieldViaReflection(
                    Build.VERSION::class.java.getDeclaredField("SDK_INT"), 30
                )
                When("Geocoder does NOT return an address") {
                    every {
                        geocoder.getFromLocation(any<Double>(), any<Double>(), 1)
                    } returns emptyList()
                    then("return a NoGeocodedAddressError") {
                        getFromLocation(0.0, 0.0) shouldBe
                            Either.Left(Failure.GeocoderError.NoGeocodedAddressError)
                    }
                }
                When("Geocoder returns an address") {
                    every {
                        geocoder.getFromLocation(any<Double>(), any<Double>(), 1)
                    } returns listOf(address)
                    then("return that address") {
                        getFromLocation(0.0, 0.0).isSuccess(address)
                    }
                }
            }
            When("Build.VERSION.SDK_INT >= TIRAMISU") {
                setStaticFieldViaReflection(
                    Build.VERSION::class.java.getDeclaredField("SDK_INT"),
                    33
                )
                When("Geocoder does NOT return an address") {
                    // TODO: Handle API 33 in Geocoding
                }
                When("Geocoder returns an address") {
                    // TODO: Handle API 33 in Geocoding
                }
            }
        }
    }

    afterSpec {
        stopKoin()
    }

})
