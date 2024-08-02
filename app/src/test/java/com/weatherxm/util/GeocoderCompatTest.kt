package com.weatherxm.util

import android.location.Address
import android.location.Geocoder
import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.util.AppBuildConfig.versionSDK
import com.weatherxm.util.GeocoderCompat.getFromLocation
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class GeocoderCompatTest : BehaviorSpec({
    val geocoder = mockk<Geocoder>()
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

        mockkStatic(Geocoder::class)
        mockkObject(AppBuildConfig)
    }

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
                every { versionSDK() } returns 32
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
                        getFromLocation(0.0, 0.0) shouldBe Either.Right(address)
                    }
                }
            }
            // FIXME: Fix it to test above Tiramisu API level
//            When("Build.VERSION.SDK_INT >= TIRAMISU") {
//                every { versionSDK() } returns 33
//            }
        }
    }

    afterSpec {
        stopKoin()
    }

})
