package com.weatherxm.data.datasource

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import com.weatherxm.data.Bundle
import com.weatherxm.data.DataError
import com.weatherxm.data.Failure
import com.weatherxm.data.Location
import com.weatherxm.data.NetworkSearchResults
import com.weatherxm.data.PublicDevice
import com.weatherxm.data.PublicHex
import com.weatherxm.data.database.dao.BaseDao
import com.weatherxm.data.database.dao.NetworkSearchRecentDao
import com.weatherxm.data.database.entities.NetworkSearchRecent
import com.weatherxm.data.repository.ExplorerRepositoryImpl.Companion.RECENTS_MAX_ENTRIES
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.explorer.SearchResult

class DatabaseExplorerDataSource(
    private val dao: NetworkSearchRecentDao
) : ExplorerDataSource {
    override suspend fun getCells(): Either<Failure, List<PublicHex>> {
        throw NotImplementedError("Won't be implemented. Ignore this.")
    }

    override suspend fun getCellDevices(index: String): Either<Failure, List<PublicDevice>> {
        throw NotImplementedError("Won't be implemented. Ignore this.")
    }

    override suspend fun getCellDevice(
        index: String,
        deviceId: String
    ): Either<Failure, PublicDevice> {
        throw NotImplementedError("Won't be implemented. Ignore this.")
    }

    override suspend fun networkSearch(
        query: String,
        exact: Boolean?,
        exclude: String?
    ): Either<Failure, NetworkSearchResults> {
        throw NotImplementedError("Won't be implemented. Ignore this.")
    }

    override suspend fun deleteAll() {
        dao.deleteAll()
    }

    override suspend fun deleteOutOfLimitRecents() {
        val recentSearches = dao.getAll()
        if (recentSearches.size >= RECENTS_MAX_ENTRIES) {
            dao.deleteOutOfLimitRecents(recentSearches[RECENTS_MAX_ENTRIES - 1].updatedAt)
        }
    }

    override suspend fun getRecentSearches(): Either<Failure, List<SearchResult>> {
        return dao.getAll()
            .map {
                SearchResult(
                    name = it.name,
                    center = Location(it.lat, it.lon),
                    addressPlace = it.addressPlace,
                    stationBundle = Bundle(
                        it.bundleName,
                        it.bundleTitle,
                        it.connectivity,
                        it.wsModel,
                        it.gwModel,
                        it.hwClass
                    ),
                    stationCellIndex = it.stationCellIndex,
                    stationId = it.stationId
                )
            }
            .right()
            // Return DatabaseMissError if list is empty
            .flatMap { b ->
                b.takeIf { it.isNotEmpty() }?.right() ?: DataError.DatabaseMissError.left()
            }
    }

    override suspend fun setRecentSearch(search: SearchResult) {
        if (search.name == null || search.center == null) return

        BaseDao.Companion.DAOWrapper(dao).insertWithTimestamp(
            NetworkSearchRecent(
                name = search.name ?: String.empty(),
                lat = search.center?.lat ?: 0.0,
                lon = search.center?.lon ?: 0.0,
                addressPlace = search.addressPlace,
                bundleName = search.stationBundle?.name,
                bundleTitle = search.stationBundle?.title,
                connectivity = search.stationBundle?.connectivity,
                wsModel = search.stationBundle?.wsModel,
                gwModel = search.stationBundle?.gwModel,
                hwClass = search.stationBundle?.hwClass,
                stationCellIndex = search.stationCellIndex,
                stationId = search.stationId,
            )
        )
    }
}
