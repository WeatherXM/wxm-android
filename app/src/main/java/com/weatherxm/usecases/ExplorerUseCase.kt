package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.Location
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.explorer.ExplorerData
import com.weatherxm.ui.explorer.SearchResult
import com.weatherxm.ui.explorer.UICell

interface ExplorerUseCase {
    companion object {
        const val DEVICE_COUNT_KEY = "device_count"
    }

    suspend fun getCells(): Either<Failure, ExplorerData>
    suspend fun getCellDevices(cell: UICell): Either<Failure, List<UIDevice>>
    suspend fun getCellDevice(index: String, deviceId: String): Either<Failure, UIDevice>
    suspend fun networkSearch(
        query: String,
        exact: Boolean? = null,
        exclude: String? = null
    ): Either<Failure, List<SearchResult>>

    suspend fun getRecentSearches(): List<SearchResult>
    suspend fun setRecentSearch(search: SearchResult)
    suspend fun getUserCountryLocation(): Location?
    suspend fun getCellInfo(index: String): Either<Failure, UICell>
}
