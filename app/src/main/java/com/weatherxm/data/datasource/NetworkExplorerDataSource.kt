package com.weatherxm.data.datasource

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.NetworkSearchResults
import com.weatherxm.data.PublicDevice
import com.weatherxm.data.PublicHex
import com.weatherxm.data.map
import com.weatherxm.data.network.ApiService
import com.weatherxm.ui.explorer.SearchResult

class NetworkExplorerDataSource(private val apiService: ApiService) : ExplorerDataSource {
    override suspend fun getPublicHexes(): Either<Failure, List<PublicHex>> {
        return apiService.getPublicHexes().map()
    }

    override suspend fun getPublicDevicesOfHex(index: String): Either<Failure, List<PublicDevice>> {
        return apiService.getPublicDevicesOfHex(index).map()
    }

    override suspend fun getPublicDevice(
        index: String,
        deviceId: String
    ): Either<Failure, PublicDevice> {
        return apiService.getPublicDevice(index, deviceId).map()
    }

    override suspend fun networkSearch(query: String): Either<Failure, NetworkSearchResults> {
        return apiService.networkSearch(query).map()
    }

    override suspend fun getRecentSearches(): Either<Failure, List<SearchResult>> {
        throw NotImplementedError("Won't be implemented. Ignore this.")
    }

    override suspend fun setRecentSearch(search: SearchResult) {
        throw NotImplementedError("Won't be implemented. Ignore this.")
    }

    override suspend fun deleteAll() {
        throw NotImplementedError("Won't be implemented. Ignore this.")
    }

    override suspend fun deleteOutOfLimitRecents() {
        throw NotImplementedError("Won't be implemented. Ignore this.")
    }
}
