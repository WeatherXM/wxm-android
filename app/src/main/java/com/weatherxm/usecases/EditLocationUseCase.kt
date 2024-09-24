package com.weatherxm.usecases

import arrow.core.Either
import com.mapbox.geojson.Point
import com.mapbox.search.result.SearchSuggestion
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.Location
import com.weatherxm.ui.common.UIDevice

interface EditLocationUseCase {
    suspend fun getSearchSuggestions(query: String): Either<Failure, List<SearchSuggestion>>
    suspend fun getSuggestionLocation(suggestion: SearchSuggestion): Either<Failure, Location>
    suspend fun getAddressFromPoint(point: Point): Either<Failure, String>
    suspend fun setLocation(
        deviceId: String,
        lat: Double,
        lon: Double
    ): Either<Failure, UIDevice>
}
