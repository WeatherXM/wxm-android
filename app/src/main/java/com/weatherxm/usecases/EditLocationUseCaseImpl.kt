package com.weatherxm.usecases

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.handleErrorWith
import arrow.core.left
import arrow.core.right
import com.mapbox.geojson.Point
import com.mapbox.search.result.SearchAddress
import com.mapbox.search.result.SearchSuggestion
import com.weatherxm.data.models.CancellationError
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.Location
import com.weatherxm.data.models.MapBoxError.ReverseGeocodingError
import com.weatherxm.data.repository.GeoLocationRepository
import com.weatherxm.data.repository.DeviceRepository
import com.weatherxm.ui.common.UIDevice

class EditLocationUseCaseImpl(
    private val geoLocationRepository: GeoLocationRepository,
    private val deviceRepository: DeviceRepository
) : EditLocationUseCase {
    override suspend fun getSearchSuggestions(
        query: String
    ): Either<Failure, List<SearchSuggestion>> {
        return geoLocationRepository.getSearchSuggestions(query)
            .handleErrorWith {
                if(it is CancellationError) {
                    Either.Right(emptyList())
                } else {
                    Either.Left(it)
                }
            }
    }

    override suspend fun getSuggestionLocation(
        suggestion: SearchSuggestion
    ): Either<Failure, Location> {
        return geoLocationRepository.getSuggestionLocation(suggestion)
    }

    override suspend fun getAddressFromPoint(point: Point): Either<Failure, String> {
        return geoLocationRepository.getAddressFromPoint(point)
            .flatMap {
                it.formattedAddress(SearchAddress.FormatStyle.Medium)?.right()
                    ?: ReverseGeocodingError.SearchResultAddressFormatError().left()
            }
    }

    override suspend fun setLocation(
        deviceId: String,
        lat: Double,
        lon: Double
    ): Either<Failure, UIDevice> {
        return deviceRepository.setLocation(deviceId, lat, lon).map {
            it.toUIDevice()
        }
    }
}
