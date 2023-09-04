package com.weatherxm.data.datasource

import arrow.core.Either
import com.mapbox.geojson.Point
import com.mapbox.search.result.SearchResult
import com.weatherxm.data.CountryAndFrequencies
import com.weatherxm.data.Failure
import com.weatherxm.data.Location
import com.weatherxm.data.services.CacheService
import java.util.Locale

/**
 * Simple shared preference database with key and value both strings.
 */
class CacheAddressDataSource(private val cacheService: CacheService) : AddressDataSource {

    override suspend fun getLocationAddress(
        hexIndex: String,
        location: Location,
        locale: Locale
    ): Either<Failure, String> {
        return cacheService.getLocationAddress(hexIndex)
    }

    override suspend fun setLocationAddress(
        hexIndex: String,
        address: String
    ): Either<Failure, Unit> {
        cacheService.setLocationAddress(hexIndex, address)
        return Either.Right(Unit)
    }

    override suspend fun getAddressFromPoint(point: Point): Either<Failure, SearchResult> {
        throw NotImplementedError("Won't be implemented. Ignore this.")
    }

    override suspend fun getCountryAndFrequencies(
        location: Location,
        locale: Locale
    ): Either<Failure, CountryAndFrequencies> {
        throw NotImplementedError("Won't be implemented. Ignore this.")
    }
}
