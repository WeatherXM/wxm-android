package com.weatherxm.data.repository

import com.weatherxm.data.datasource.LocationsDataSource
import com.weatherxm.data.models.Location

interface LocationsRepository {
    fun getSavedLocations(): List<Location>
    fun addSavedLocation(location: Location)
    fun removeSavedLocation(location: Location)
}

class LocationsRepositoryImpl(
    private val dataSource: LocationsDataSource
) : LocationsRepository {
    override fun getSavedLocations(): List<Location> = dataSource.getSavedLocations()
    override fun addSavedLocation(location: Location) = dataSource.addSavedLocation(location)
    override fun removeSavedLocation(location: Location) = dataSource.removeSavedLocation(location)
}
