package com.weatherxm.data.datasource

import arrow.core.Either
import com.mapbox.geojson.Point
import com.mapbox.search.result.SearchResult
import com.weatherxm.data.CountryAndFrequencies
import com.weatherxm.data.Failure
import com.weatherxm.data.Location

interface AddressDataSource {
    suspend fun getLocationAddress(hexIndex: String, location: Location): Either<Failure, String?>
    suspend fun setLocationAddress(hexIndex: String, address: String): Either<Failure, Unit>
    suspend fun getAddressFromPoint(point: Point): Either<Failure, SearchResult>
    suspend fun getCountryAndFrequencies(location: Location): Either<Failure, CountryAndFrequencies>
}
