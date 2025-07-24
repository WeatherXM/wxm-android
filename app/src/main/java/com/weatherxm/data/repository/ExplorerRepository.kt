package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.NetworkSearchResults
import com.weatherxm.data.models.PublicDevice
import com.weatherxm.data.models.PublicHex
import com.weatherxm.data.datasource.DatabaseExplorerDataSource
import com.weatherxm.data.datasource.NetworkExplorerDataSource
import com.weatherxm.ui.home.explorer.SearchResult

interface ExplorerRepository {
    suspend fun getCells(): Either<Failure, List<PublicHex>>
    suspend fun getCellDevices(index: String): Either<Failure, List<PublicDevice>>
    suspend fun getCellDevice(index: String, deviceId: String): Either<Failure, PublicDevice>
    suspend fun networkSearch(
        query: String,
        exact: Boolean? = null,
        exclude: String? = null
    ): Either<Failure, NetworkSearchResults>

    suspend fun getRecentSearches(): Either<Failure, List<SearchResult>>
    suspend fun setRecentSearch(search: SearchResult)
}

class ExplorerRepositoryImpl(
    private val networkExplorerDataSource: NetworkExplorerDataSource,
    private val databaseExplorerDataSource: DatabaseExplorerDataSource
) : ExplorerRepository {
    companion object {
        const val RECENTS_MAX_ENTRIES = 10
        const val EXCLUDE_PLACES = "places"
    }

    override suspend fun getCells(): Either<Failure, List<PublicHex>> {
        return networkExplorerDataSource.getCells()
    }

    override suspend fun getCellDevices(index: String): Either<Failure, List<PublicDevice>> {
        return networkExplorerDataSource.getCellDevices(index)
    }

    override suspend fun getCellDevice(
        index: String,
        deviceId: String
    ): Either<Failure, PublicDevice> {
        return networkExplorerDataSource.getCellDevice(index, deviceId)
    }

    override suspend fun networkSearch(
        query: String,
        exact: Boolean?,
        exclude: String?
    ): Either<Failure, NetworkSearchResults> {
        return networkExplorerDataSource.networkSearch(query.trim(), exact, exclude)
    }

    override suspend fun getRecentSearches(): Either<Failure, List<SearchResult>> {
        return databaseExplorerDataSource.getRecentSearches()
    }

    override suspend fun setRecentSearch(search: SearchResult) {
        getRecentSearches().onRight {
            if (it.size >= RECENTS_MAX_ENTRIES) {
                databaseExplorerDataSource.deleteOutOfLimitRecents()
            }
        }
        databaseExplorerDataSource.setRecentSearch(search)
    }
}
