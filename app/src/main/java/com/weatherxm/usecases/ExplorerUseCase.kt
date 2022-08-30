package com.weatherxm.usecases

import arrow.core.Either
import com.google.gson.Gson
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotationOptions
import com.weatherxm.R
import com.weatherxm.data.Failure
import com.weatherxm.data.Location
import com.weatherxm.data.repository.ExplorerRepository
import com.weatherxm.data.repository.TokenRepository
import com.weatherxm.ui.ExplorerData
import com.weatherxm.ui.TokenInfo
import com.weatherxm.ui.UIDevice
import com.weatherxm.ui.UIHex
import com.weatherxm.ui.explorer.ExplorerViewModel.Companion.FILL_OPACITY_HEXAGONS
import com.weatherxm.ui.explorer.ExplorerViewModel.Companion.HEATMAP_SOURCE_ID
import com.weatherxm.util.DateTimeHelper.getLocalDate
import com.weatherxm.util.DateTimeHelper.getNowInTimezone
import com.weatherxm.util.DateTimeHelper.getTimezone
import com.weatherxm.util.ResourcesHelper


interface ExplorerUseCase {
    companion object {
        const val DEVICE_COUNT_KEY = "device_count"
    }

    fun polygonPointsToLatLng(pointsOfPolygon: List<Location>): List<MutableList<Point>>
    suspend fun getPublicHexes(): Either<Failure, ExplorerData>
    suspend fun getPublicDevicesOfHex(uiHex: UIHex): Either<Failure, List<UIDevice>>
    suspend fun getPublicDevice(index: String, deviceId: String): Either<Failure, UIDevice>
    suspend fun getTokenInfoLast30D(deviceId: String): Either<Failure, TokenInfo>
}

class ExplorerUseCaseImpl(
    private val explorerRepository: ExplorerRepository,
    private val tokenRepository: TokenRepository,
    private val gson: Gson,
    private val resHelper: ResourcesHelper
) : ExplorerUseCase {
    // Points and heatmap to paint
    private var heatmap: GeoJsonSource = geoJsonSource(HEATMAP_SOURCE_ID)

    override fun polygonPointsToLatLng(pointsOfPolygon: List<Location>): List<MutableList<Point>> {
        val latLongs = listOf(pointsOfPolygon.map { coordinates ->
            Point.fromLngLat(coordinates.lon, coordinates.lat)
        }.toMutableList())

        // Custom/Temporary fix for: https://github.com/mapbox/mapbox-maps-android/issues/733
        latLongs.map { coordinates ->
            coordinates.add(coordinates[0])
        }
        return latLongs
    }

    override suspend fun getPublicHexes(): Either<Failure, ExplorerData> {
        return explorerRepository.getPublicHexes().map {
            val geoJsonSource = heatmap.featureCollection(FeatureCollection.fromFeatures(
                it.map { hex ->
                    Feature.fromGeometry(Point.fromLngLat(hex.center.lon, hex.center.lat)).apply {
                        this.addNumberProperty(ExplorerUseCase.DEVICE_COUNT_KEY, hex.deviceCount)
                    }
                }
            ))

            var totalDevices = 0
            val polygonPoints = it.map { publicHex ->
                totalDevices += publicHex.deviceCount ?: 0

                PolygonAnnotationOptions()
                    .withFillColor(resHelper.getColor(R.color.hexFillColor))
                    .withFillOpacity(FILL_OPACITY_HEXAGONS)
                    .withFillOutlineColor(resHelper.getColor(R.color.hexFillOutlineColor))
                    .withData(gson.toJsonTree(UIHex(publicHex.index, publicHex.center)))
                    .withPoints(polygonPointsToLatLng(publicHex.polygon))
            }

            ExplorerData(totalDevices, geoJsonSource, polygonPoints)
        }
    }

    override suspend fun getPublicDevicesOfHex(uiHex: UIHex): Either<Failure, List<UIDevice>> {
        return explorerRepository.getPublicDevicesOfHex(uiHex.index).map {
            val address = explorerRepository.getAddressFromLocation(uiHex.index, uiHex.center)

            it.map { publicDevice ->
                publicDevice.toUIDevice().apply {
                    this.address = address
                }
            }.sortedWith(
                compareByDescending<UIDevice> { it.lastWeatherStationActivity }.thenBy { it.name }
            )
        }
    }

    override suspend fun getPublicDevice(
        index: String,
        deviceId: String
    ): Either<Failure, UIDevice> {
        return explorerRepository.getPublicDevice(index, deviceId).map {
            it.toUIDevice()
        }
    }

    // We suppress magic number because we use specific numbers to check last month and last week
    @Suppress("MagicNumber")
    override suspend fun getTokenInfoLast30D(deviceId: String): Either<Failure, TokenInfo> {
        val now = getNowInTimezone()
        // Last 29 days of transactions + today = 30 days
        val fromDate = getLocalDate(now.minusDays(28).toString()).toString()
        val timezone = getTimezone()

        return tokenRepository.getAllPublicTransactionsInRange(deviceId, timezone, fromDate)
            .map {
                TokenInfo().fromLastAndDatedTxs(it)
            }
    }
}

