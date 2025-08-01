package com.weatherxm.data.datasource

import com.weatherxm.TestConfig.cacheService
import com.weatherxm.data.locationToText
import com.weatherxm.data.models.Location
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.verify

class LocationsDataSourceTest : BehaviorSpec({
    val datasource = LocationsDataSourceImpl(cacheService)

    val location = Location.empty()
    val savedLocationsInCache = setOf(location.locationToText())
    val savedLocations = listOf(location)

    beforeSpec {
        justRun { cacheService.setSavedLocations(any()) }
    }

    context("GET saved locations") {
        given("a datasource providing them") {
            every { cacheService.getSavedLocations() } returns savedLocationsInCache
            then("return them") {
                datasource.getSavedLocations() shouldBe savedLocations
            }
        }
    }

    context("Add a saved location") {
        given("A Cache Source providing the SAVE mechanism") {
            When("Using the Cache Source") {
                every { cacheService.getSavedLocations() } returns emptySet()
                then("add in this list from the cache the location and save that list") {
                    datasource.addSavedLocation(location)
                    verify(exactly = 1) {
                        cacheService.setSavedLocations(listOf(location.locationToText()))
                    }
                }
            }
        }
    }

    context("Remove a saved location") {
        given("A Cache Source providing the SAVE mechanism") {
            When("Using the Cache Source") {
                every { cacheService.getSavedLocations() } returns savedLocationsInCache
                then("remove this from the cache and save that list again") {
                    datasource.removeSavedLocation(location)
                    verify(exactly = 1) {
                        cacheService.setSavedLocations(listOf())
                    }
                }
            }
        }
    }
})
