package com.weatherxm.data.datasource

import com.weatherxm.data.locationToText
import com.weatherxm.data.models.Location
import com.weatherxm.data.services.CacheService
import com.weatherxm.data.textToLocation

interface LocationsDataSource {
    fun getSavedLocations(): List<Location>
    fun addSavedLocation(location: Location)
    fun removeSavedLocation(location: Location)

    companion object {
        const val MAX_AUTH_LOCATIONS = 9
    }
}

class LocationsDataSourceImpl(
    private val cacheService: CacheService
) : LocationsDataSource {
    override fun getSavedLocations(): List<Location> {
        return cacheService.getSavedLocations().map {
            it.textToLocation()
        }
    }

    override fun addSavedLocation(location: Location) {
        val currentSavedLocations = cacheService.getSavedLocations().toMutableList()
        val locationAsText = location.locationToText()
        if (!currentSavedLocations.contains(locationAsText)) {
            currentSavedLocations.add(locationAsText)
        }
        cacheService.setSavedLocations(currentSavedLocations)
    }

    override fun removeSavedLocation(location: Location) {
        val currentSavedLocations = cacheService.getSavedLocations().toMutableList()
        val locationAsText = location.locationToText()
        if (currentSavedLocations.contains(locationAsText)) {
            currentSavedLocations.remove(locationAsText)
        }
        cacheService.setSavedLocations(currentSavedLocations)
    }
}
