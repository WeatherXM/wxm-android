package com.weatherxm.ui.claimdevice.location

import com.weatherxm.data.models.Location
import com.weatherxm.ui.InstantExecutorListener
import com.weatherxm.ui.common.DeviceType
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class ClaimLocationViewModelTest : BehaviorSpec({
    val viewModel = ClaimLocationViewModel()

    val deviceType = DeviceType.D1_WIFI
    val lat = 10.0
    val lon = 10.0

    listener(InstantExecutorListener())

    context("SET a Device Type and then GET it") {
        given("a device type") {
            then("ensure that the current one is the default one") {
                viewModel.getDeviceType() shouldBe DeviceType.M5_WIFI
            }
            and("SET it") {
                viewModel.setDeviceType(deviceType)
                then("GET it and ensure it's set correctly") {
                    viewModel.getDeviceType() shouldBe deviceType
                }
            }
        }
    }

    context("SET an installation location and then GET it") {
        given("a claiming key") {
            then("ensure that the current one is the default one") {
                viewModel.getInstallationLocation() shouldBe Location.empty()
            }
            and("SET it") {
                viewModel.setInstallationLocation(lat, lon)
                then("GET it and ensure it's set correctly") {
                    viewModel.getInstallationLocation() shouldBe Location(lat, lon)
                }
            }
        }
    }

    context("Get user's location") {
        given("a request to the user to provide it") {
            then("LiveData onRequestUserLocation should post the value true") {
                viewModel.onRequestUserLocation().value shouldBe false
                viewModel.requestUserLocation()
                viewModel.onRequestUserLocation().value shouldBe true
            }
        }
    }
})
