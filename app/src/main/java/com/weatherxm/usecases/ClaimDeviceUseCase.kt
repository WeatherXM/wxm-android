package com.weatherxm.usecases

import android.location.Location
import arrow.core.Either
import arrow.core.flatMap
import arrow.core.handleErrorWith
import arrow.core.rightIfNotNull
import com.mapbox.geojson.Point
import com.mapbox.search.result.SearchAddress
import com.mapbox.search.result.SearchSuggestion
import com.weatherxm.data.CancellationError
import com.weatherxm.data.CountryAndFrequencies
import com.weatherxm.data.Device
import com.weatherxm.data.Failure
import com.weatherxm.data.MapBoxError.ReverseGeocodingError
import com.weatherxm.data.repository.AddressRepository
import com.weatherxm.data.repository.DeviceRepository
import com.weatherxm.data.repository.UserRepository

interface ClaimDeviceUseCase {
    suspend fun claimDevice(serialNumber: String, lat: Double, lon: Double): Either<Failure, Device>
    suspend fun fetchUserEmail(): Either<Failure, String>
    suspend fun getSearchSuggestions(query: String): Either<Failure, List<SearchSuggestion>>
    suspend fun getSuggestionLocation(suggestion: SearchSuggestion): Either<Failure, Location>
    suspend fun getAddressFromPoint(point: Point): Either<Failure, String>
    suspend fun getCountryAndFrequencies(location: Location): CountryAndFrequencies
}

class ClaimDeviceUseCaseImpl(
    private val deviceRepository: DeviceRepository,
    private val userRepository: UserRepository,
    private val addressRepository: AddressRepository
) : ClaimDeviceUseCase {

    override suspend fun claimDevice(
        serialNumber: String,
        lat: Double,
        lon: Double
    ): Either<Failure, Device> {
        return deviceRepository.claimDevice(serialNumber, com.weatherxm.data.Location(lat, lon))
    }

    override suspend fun fetchUserEmail(): Either<Failure, String> {
        return userRepository.getUserUsername()
    }

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
                it.formattedAddress(SearchAddress.FormatStyle.Medium).rightIfNotNull {
                    ReverseGeocodingError.SearchResultAddressFormatError
                }
            }
    }

    override suspend fun getCountryAndFrequencies(location: Location): CountryAndFrequencies {
        return addressRepository.getCountryAndFrequencies(location)
    }
}
