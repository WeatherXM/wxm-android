package com.weatherxm.usecases

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.getOrElse
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.weatherxm.data.models.DataError
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.Location
import com.weatherxm.data.repository.DeviceRepository
import com.weatherxm.data.repository.ExplorerRepository
import com.weatherxm.data.repository.FollowRepository
import com.weatherxm.data.repository.LocationRepository
import com.weatherxm.ui.common.DeviceRelation
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.explorer.ExplorerData
import com.weatherxm.ui.explorer.ExplorerViewModel.Companion.HEATMAP_SOURCE_ID
import com.weatherxm.ui.explorer.SearchResult
import com.weatherxm.ui.explorer.UICell

@Suppress("LongParameterList")
class ExplorerUseCaseImpl(
    private val explorerRepository: ExplorerRepository,
    private val followRepository: FollowRepository,
    private val deviceRepository: DeviceRepository,
    private val locationRepository: LocationRepository
) : ExplorerUseCase {
    // Points and heatmap to paint
    private var heatmap: GeoJsonSource = geoJsonSource(HEATMAP_SOURCE_ID)

    override suspend fun getCellInfo(index: String): Either<Failure, UICell> {
        return explorerRepository.getCells().flatMap {
            it.firstOrNull { cell ->
                cell.index == index
            }?.let { cell ->
                Either.Right(UICell(cell.index, cell.center))
            } ?: Either.Left(DataError.CellNotFound)
        }
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

            ExplorerData(geoJsonSource, it)
        }
    }

    override suspend fun getCellDevices(cell: UICell): Either<Failure, List<UIDevice>> {
        return explorerRepository.getCellDevices(cell.index).map { devices ->
            devices.map { publicDevice ->
                publicDevice.toUIDevice().apply {
                    this.cellCenter = cell.center
                    this.relation = getRelation(this.id)
                    createDeviceAlerts(false)
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
                this.relation = getRelation(this.id)
            }
        }
    }

    override suspend fun networkSearch(
        query: String,
        exact: Boolean?,
        exclude: String?
    ): Either<Failure, List<SearchResult>> {
        return explorerRepository.networkSearch(query, exact, exclude).map {
            mutableListOf<SearchResult>().apply {
                addAll(
                    it.devices?.map { device ->
                        SearchResult(
                            name = device.name,
                            center = device.cellCenter,
                            stationId = device.id,
                            stationCellIndex = device.cellIndex,
                            stationBundle = device.bundle,
                            relation = getRelation(device.id)
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
        return explorerRepository.getRecentSearches().getOrElse { mutableListOf() }.onEach {
            it.relation = getRelation(it.stationId)
        }
    }

    override suspend fun setRecentSearch(search: SearchResult) {
        explorerRepository.setRecentSearch(search)
    }

    override suspend fun getUserCountryLocation(): Location? {
        return locationRepository.getUserCountryLocation()
    }

    private suspend fun getRelation(deviceId: String?): DeviceRelation {
        val followedDevices = followRepository.getFollowedDevicesIds()
        val userDevices = deviceRepository.getUserDevicesIds()
        return if (userDevices.contains(deviceId)) {
            DeviceRelation.OWNED
        } else if (followedDevices.contains(deviceId)) {
            DeviceRelation.FOLLOWED
        } else {
            DeviceRelation.UNFOLLOWED
        }
    }
}

