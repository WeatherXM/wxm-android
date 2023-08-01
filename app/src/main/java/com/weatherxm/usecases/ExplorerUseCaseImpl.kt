package com.weatherxm.usecases

import arrow.core.Either
import arrow.core.getOrElse
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
import com.weatherxm.data.repository.AddressRepository
import com.weatherxm.data.repository.ExplorerRepository
import com.weatherxm.ui.common.DeviceOwnershipStatus
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.explorer.ExplorerData
import com.weatherxm.ui.explorer.ExplorerViewModel.Companion.FILL_OPACITY_HEXAGONS
import com.weatherxm.ui.explorer.ExplorerViewModel.Companion.HEATMAP_SOURCE_ID
import com.weatherxm.ui.explorer.SearchResult
import com.weatherxm.ui.explorer.UICell
import com.weatherxm.util.ResourcesHelper

class ExplorerUseCaseImpl(
    private val explorerRepository: ExplorerRepository,
    private val addressRepository: AddressRepository,
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

    override suspend fun getCells(): Either<Failure, ExplorerData> {
        return explorerRepository.getCells().map {
            val geoJsonSource = heatmap.featureCollection(FeatureCollection.fromFeatures(
                it.map { hex ->
                    Feature.fromGeometry(Point.fromLngLat(hex.center.lon, hex.center.lat)).apply {
                        this.addNumberProperty(
                            ExplorerUseCase.DEVICE_COUNT_KEY,
                            hex.deviceCount
                        )
                    }
                }
            ))

            val polygonPoints = it.map { publicHex ->
                PolygonAnnotationOptions()
                    .withFillColor(resHelper.getColor(R.color.hex_fill_color))
                    .withFillOpacity(FILL_OPACITY_HEXAGONS)
                    .withFillOutlineColor(resHelper.getColor(R.color.white))
                    .withData(gson.toJsonTree(UICell(publicHex.index, publicHex.center)))
                    .withPoints(polygonPointsToLatLng(publicHex.polygon))
            }

            ExplorerData(geoJsonSource, polygonPoints)
        }
    }

    override suspend fun getCellDevices(cell: UICell): Either<Failure, List<UIDevice>> {
        return explorerRepository.getCellDevices(cell.index).map {
            val address = addressRepository.getAddressFromLocation(cell.index, cell.center)

            it.map { publicDevice ->
                publicDevice.toUIDevice().apply {
                    this.cellCenter = cell.center
                    this.address = address
                    // TODO: Change this when we implement the feature
                    this.ownershipStatus = DeviceOwnershipStatus.UNFOLLOWED
                }
            }.sortedWith(
                compareByDescending<UIDevice> { it.lastWeatherStationActivity }.thenBy { it.name }
            )
        }
    }

    override suspend fun getCellDevice(
        index: String,
        deviceId: String
    ): Either<Failure, UIDevice> {
        return explorerRepository.getCellDevice(index, deviceId).map {
            it.toUIDevice().apply {
                // TODO: Change this when we implement the feature
                this.ownershipStatus = DeviceOwnershipStatus.UNFOLLOWED
            }
        }
    }

    override suspend fun networkSearch(
        query: String,
        exact: Boolean?,
        exclude: String?
    ): Either<Failure, List<SearchResult>> {
        return explorerRepository.networkSearch(query).map {
            mutableListOf<SearchResult>().apply {
                addAll(
                    it.devices?.map { device ->
                        SearchResult(
                            name = device.name,
                            center = device.cellCenter,
                            stationId = device.id,
                            stationCellIndex = device.cellIndex,
                            stationConnectivity = device.connectivity
                        )
                    } ?: mutableListOf())

                addAll(
                    it.addresses?.map { address ->
                        SearchResult(
                            name = address.name,
                            addressPlace = address.place,
                            center = address.center
                        )
                    } ?: mutableListOf())
            }
        }
    }

    override suspend fun getRecentSearches(): List<SearchResult> {
        return explorerRepository.getRecentSearches().getOrElse { mutableListOf() }
    }

    override suspend fun setRecentSearch(search: SearchResult) {
        explorerRepository.setRecentSearch(search)
    }
}

