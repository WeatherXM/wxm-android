package com.weatherxm.data.datasource

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.NetworkSearchResults
import com.weatherxm.data.PublicDevice
import com.weatherxm.data.PublicHex
import com.weatherxm.ui.explorer.SearchResult

interface ExplorerDataSource {
    suspend fun getPublicHexes(): Either<Failure, List<PublicHex>>
    suspend fun getPublicDevicesOfHex(index: String): Either<Failure, List<PublicDevice>>
    suspend fun getPublicDevice(index: String, deviceId: String): Either<Failure, PublicDevice>
    suspend fun networkSearch(query: String): Either<Failure, NetworkSearchResults>
    suspend fun getRecentSearches(): Either<Failure, List<SearchResult>>
    suspend fun setRecentSearch(search: SearchResult)
    suspend fun deleteAll()
    suspend fun deleteOutOfLimitRecents()
}
