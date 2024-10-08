package com.weatherxm.data.datasource

import arrow.core.Either
import com.mapbox.geojson.Point
import com.mapbox.search.result.SearchResult
import com.weatherxm.data.models.CountryAndFrequencies
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.Location
import com.weatherxm.data.services.CacheService

/**
 * Simple shared preference database with key and value both strings.
 */
class CacheAddressDataSource(private val cacheService: CacheService) : AddressDataSource {

    override suspend fun getLocationAddress(
        hexIndex: String,
        location: Location
    ): Either<Failure, String> {
        return cacheService.getLocationAddress(hexIndex)
    }

    override suspend fun setLocationAddress(hexIndex: String, address: String) {
        cacheService.setLocationAddress(hexIndex, address)
    }

    override suspend fun getAddressFromPoint(point: Point): Either<Failure, SearchResult> {
        throw NotImplementedError("Won't be implemented. Ignore this.")
    }

    override suspend fun getCountryAndFrequencies(
        location: Location
    ): Either<Failure, CountryAndFrequencies> {
        throw NotImplementedError("Won't be implemented. Ignore this.")
    }
}
