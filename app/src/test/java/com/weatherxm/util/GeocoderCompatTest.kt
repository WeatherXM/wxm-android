package com.weatherxm.util

import android.location.Address
import android.location.Geocoder
import android.os.Build
import arrow.core.Either
import com.weatherxm.TestConfig.geocoder
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.data.models.Failure
import com.weatherxm.util.GeocoderCompat.getFromLocation
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class GeocoderCompatTest : BehaviorSpec({
    val address = mockk<Address>()
    val listenerSlot = slot<Geocoder.GeocodeListener>()

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
            every { AndroidBuildInfo.sdkInt } returns Build.VERSION_CODES.TIRAMISU - 1
            When("Build.VERSION.SDK_INT < TIRAMISU") {
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
                every { AndroidBuildInfo.sdkInt } returns Build.VERSION_CODES.TIRAMISU
                When("Geocoder does NOT return an address") {
                    every {
                        geocoder.getFromLocation(0.0, 0.0, 1, capture(listenerSlot))
                    } answers {
                        listenerSlot.captured.onGeocode(listOf())
                    }
                    then("return a NoGeocodedAddressError") {
                        getFromLocation(0.0, 0.0) shouldBe
                            Either.Left(Failure.GeocoderError.NoGeocodedAddressError)
                    }
                }
                When("Geocoder returns an address") {
                    every {
                        geocoder.getFromLocation(0.0, 0.0, 1, capture(listenerSlot))
                    } answers {
                        listenerSlot.captured.onGeocode(listOf(address))
                    }
                    then("return that address") {
                        getFromLocation(0.0, 0.0).isSuccess(address)
                    }
                }
            }
        }
    }

    afterSpec {
        stopKoin()
    }

})
