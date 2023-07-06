package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.Location
import com.weatherxm.data.NetworkSearchResults
import com.weatherxm.data.PublicDevice
import com.weatherxm.data.PublicHex
import com.weatherxm.data.datasource.CacheAddressDataSource
import com.weatherxm.data.datasource.DatabaseExplorerDataSource
import com.weatherxm.data.datasource.NetworkAddressDataSource
import com.weatherxm.data.datasource.NetworkExplorerDataSource
import com.weatherxm.ui.explorer.SearchResult
import timber.log.Timber

interface ExplorerRepository {
    suspend fun getPublicHexes(): Either<Failure, List<PublicHex>>
    suspend fun getPublicDevicesOfHex(index: String): Either<Failure, List<PublicDevice>>
    suspend fun getPublicDevice(index: String, deviceId: String): Either<Failure, PublicDevice>
    suspend fun getAddressFromLocation(hexIndex: String, location: Location): String?
    suspend fun networkSearch(query: String): Either<Failure, NetworkSearchResults>
    suspend fun getRecentSearches(): Either<Failure, List<SearchResult>>
    suspend fun setRecentSearch(search: SearchResult)
}

class ExplorerRepositoryImpl(
    private val networkExplorerDataSource: NetworkExplorerDataSource,
    private val databaseExplorerDataSource: DatabaseExplorerDataSource,
    private val networkAddressDataSource: NetworkAddressDataSource,
    private val cacheAddressDataSource: CacheAddressDataSource
) : ExplorerRepository {
    companion object {
        const val RECENTS_MAX_ENTRIES = 10
    }

    override suspend fun getPublicHexes(): Either<Failure, List<PublicHex>> {
        return networkExplorerDataSource.getPublicHexes()
    }

    override suspend fun getPublicDevicesOfHex(index: String): Either<Failure, List<PublicDevice>> {
        return networkExplorerDataSource.getPublicDevicesOfHex(index)
    }

    override suspend fun getPublicDevice(
        index: String,
        deviceId: String
    ): Either<Failure, PublicDevice> {
        return networkExplorerDataSource.getPublicDevice(index, deviceId)
    }

    override suspend fun getAddressFromLocation(hexIndex: String, location: Location): String? {
        var hexAddress: String? = null

        cacheAddressDataSource.getLocationAddress(hexIndex, location)
            .onRight { address ->
                Timber.d("Got location address from database [$address].")
                hexAddress = address
            }
            .mapLeft {
                networkAddressDataSource.getLocationAddress(hexIndex, location).onRight { address ->
                    Timber.d("Got location address from network [$it].")
                    hexAddress = address
                    address?.let {
                        cacheAddressDataSource.setLocationAddress(hexIndex, it)
                    }
                }
            }

        return hexAddress
    }

    override suspend fun networkSearch(query: String): Either<Failure, NetworkSearchResults> {
        return networkExplorerDataSource.networkSearch(query.trim())
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
