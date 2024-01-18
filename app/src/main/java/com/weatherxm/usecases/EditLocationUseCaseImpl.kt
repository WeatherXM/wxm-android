package com.weatherxm.usecases

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.handleErrorWith
import arrow.core.left
import arrow.core.right
import com.mapbox.geojson.Point
import com.mapbox.search.result.SearchAddress
import com.mapbox.search.result.SearchSuggestion
import com.weatherxm.data.CancellationError
import com.weatherxm.data.Failure
import com.weatherxm.data.Location
import com.weatherxm.data.MapBoxError.ReverseGeocodingError
import com.weatherxm.data.repository.AddressRepository
import com.weatherxm.data.repository.DeviceRepository
import com.weatherxm.ui.common.UIDevice

class EditLocationUseCaseImpl(
    private val addressRepository: AddressRepository,
    private val deviceRepository: DeviceRepository
) : EditLocationUseCase {
    override suspend fun getSearchSuggestions(
        query: String
    ): Either<Failure, List<SearchSuggestion>> {
        return addressRepository.getSearchSuggestions(query)
            .handleErrorWith {
                when (it) {
                    is CancellationError -> Either.Right(emptyList())
                    else -> Either.Left(it)
                }
            }
    }

    override suspend fun getSuggestionLocation(
        suggestion: SearchSuggestion
    ): Either<Failure, Location> {
        return addressRepository.getSuggestionLocation(suggestion)
    }

    override suspend fun getAddressFromPoint(point: Point): Either<Failure, String> {
        return addressRepository.getAddressFromPoint(point)
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
