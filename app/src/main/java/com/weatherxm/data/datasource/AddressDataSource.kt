package com.weatherxm.data.datasource

import arrow.core.Either
import com.mapbox.geojson.Point
import com.mapbox.search.result.SearchResult
import com.weatherxm.data.CountryAndFrequencies
import com.weatherxm.data.Failure
import com.weatherxm.data.Location
import java.util.*

interface AddressDataSource {
    suspend fun getLocationAddress(
        hexIndex: String,
        location: Location,
        locale: Locale = Locale.getDefault()
    ): Either<Failure, String?>

    suspend fun setLocationAddress(hexIndex: String, address: String): Either<Failure, Unit>

    suspend fun getAddressFromPoint(point: Point): Either<Failure, SearchResult>
    suspend fun getCountryAndFrequencies(
        location: android.location.Location,
        locale: Locale = Locale.getDefault()
    ): Either<Failure, CountryAndFrequencies>
}
