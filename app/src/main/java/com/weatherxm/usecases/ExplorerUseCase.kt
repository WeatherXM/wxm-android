package com.weatherxm.usecases

import arrow.core.Either
import com.mapbox.geojson.Point
import com.weatherxm.data.Failure
import com.weatherxm.data.Location
import com.weatherxm.ui.common.TokenInfo
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.explorer.ExplorerData
import com.weatherxm.ui.explorer.SearchResult
import com.weatherxm.ui.explorer.UIHex


interface ExplorerUseCase {
    companion object {
        const val DEVICE_COUNT_KEY = "device_count"
    }

    fun polygonPointsToLatLng(pointsOfPolygon: List<Location>): List<MutableList<Point>>
    suspend fun getPublicHexes(): Either<Failure, ExplorerData>
    suspend fun getPublicDevicesOfHex(uiHex: UIHex): Either<Failure, List<UIDevice>>
    suspend fun getPublicDevice(index: String, deviceId: String): Either<Failure, UIDevice>
    suspend fun getTokenInfoLast30D(deviceId: String): Either<Failure, TokenInfo>
    suspend fun networkSearch(query: String): Either<Failure, List<SearchResult>>
    suspend fun getRecentSearches(): List<SearchResult>
    suspend fun setRecentSearch(search: SearchResult)
    suspend fun getAddressOfHex(uiHex: UIHex): String?
}
