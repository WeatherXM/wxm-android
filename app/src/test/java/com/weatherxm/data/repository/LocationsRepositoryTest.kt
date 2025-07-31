package com.weatherxm.data.repository

import com.weatherxm.data.datasource.LocationsDataSource
import com.weatherxm.data.models.Location
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify

class LocationsRepositoryTest : BehaviorSpec({
    val dataSource = mockk<LocationsDataSource>()
    val repo = LocationsRepositoryImpl(dataSource)

    val location = Location.empty()
    val savedLocations = listOf(location)

    beforeSpec {
        every { dataSource.getSavedLocations() } returns savedLocations
        justRun { dataSource.addSavedLocation(Location.empty()) }
        justRun { dataSource.removeSavedLocation(Location.empty()) }
    }

    context("GET saved locations") {
        given("a datasource providing them") {
            then("return them") {
                repo.getSavedLocations() shouldBe savedLocations
            }
        }
    }

    context("ADD / REMOVE the location from the saved ones") {
        When("ADD") {
            repo.addSavedLocation(location)
            then("call the respective function in the usecase") {
                verify(exactly = 1) { dataSource.addSavedLocation(location) }
            }
        }
        When("REMOVE") {
            repo.removeSavedLocation(location)
            then("call the respective function in the usecase") {
                verify(exactly = 1) { dataSource.removeSavedLocation(location) }
            }
        }
    }
})
