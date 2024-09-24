package com.weatherxm.data.datasource

import arrow.core.Either
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.NetworkSearchResults
import com.weatherxm.data.models.PublicDevice
import com.weatherxm.data.models.PublicHex
import com.weatherxm.data.mapResponse
import com.weatherxm.data.network.ApiService
import com.weatherxm.ui.explorer.SearchResult

class NetworkExplorerDataSource(private val apiService: ApiService) : ExplorerDataSource {
    override suspend fun getCells(): Either<Failure, List<PublicHex>> {
        return apiService.getCells().mapResponse()
    }

    override suspend fun getCellDevices(index: String): Either<Failure, List<PublicDevice>> {
        return apiService.getCellDevices(index).mapResponse()
    }

    override suspend fun getCellDevice(
        index: String,
        deviceId: String
    ): Either<Failure, PublicDevice> {
        return apiService.getCellDevice(index, deviceId).mapResponse()
    }

    override suspend fun networkSearch(
        query: String,
        exact: Boolean?,
        exclude: String?
    ): Either<Failure, NetworkSearchResults> {
        return apiService.networkSearch(query, exact, exclude).mapResponse()
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
