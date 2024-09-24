package com.weatherxm.data.datasource

import arrow.core.Either
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.NetworkSearchResults
import com.weatherxm.data.models.PublicDevice
import com.weatherxm.data.models.PublicHex
import com.weatherxm.ui.explorer.SearchResult

interface ExplorerDataSource {
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
    suspend fun deleteAll()
    suspend fun deleteOutOfLimitRecents()
}
