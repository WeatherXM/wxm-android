package com.weatherxm.usecases

import arrow.core.Either
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.mapbox.geojson.Point
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotationOptions
import com.squareup.moshi.Moshi
import com.weatherxm.R
import com.weatherxm.data.Failure
import com.weatherxm.data.Location
import com.weatherxm.data.PublicDevice
import com.weatherxm.data.repository.DeviceRepository
import com.weatherxm.ui.explorer.DeviceWithResolution
import com.weatherxm.ui.explorer.ExplorerViewModel.Companion.FILL_OPACITY_HEXAGONS
import com.weatherxm.ui.explorer.ExplorerViewModel.Companion.H3_RESOLUTION
import com.weatherxm.ui.explorer.ExplorerViewModel.Companion.H7_RESOLUTION
import com.weatherxm.ui.explorer.ExplorerViewModel.Companion.ZOOM_LEVEL_CHANGE_HEX
import com.weatherxm.util.ResourcesHelper
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

interface ExplorerUseCase {
    fun polygonPointsToLatLng(pointsOfPolygon: Array<Location>): List<MutableList<Point>>
    suspend fun getPointsFromPublicDevices(zoom: Double): Either<Failure, List<PolygonAnnotationOptions>>
    fun getCenterOfHex3(deviceWithResolution: DeviceWithResolution?): Point?
    fun deviceWithResToJson(device: PublicDevice, resolution: Int): JsonElement
    suspend fun saveDevicesPoints(devices: List<PublicDevice>)
}

class ExplorerUseCaseImpl : ExplorerUseCase, KoinComponent {

    private val deviceRepository: DeviceRepository by inject()
    private val moshi: Moshi by inject()
    private val gson: Gson by inject()
    private val resourcesHelper: ResourcesHelper by inject()

    private var pointsHex7: MutableList<PolygonAnnotationOptions> = mutableListOf()
    private var pointsHex3: MutableList<PolygonAnnotationOptions> = mutableListOf()

    // Already painted H3 hexes, their indexes are saved here
    private val currentH3Hexes: MutableList<String> = mutableListOf()

    override fun polygonPointsToLatLng(pointsOfPolygon: Array<Location>): List<MutableList<Point>> {
        val latLongs = listOf(pointsOfPolygon.map { coordinates ->
            Point.fromLngLat(coordinates.lon, coordinates.lat)
        }.toMutableList())

        // Custom/Temporary fix for: https://github.com/mapbox/mapbox-maps-android/issues/733
        latLongs.map { coordinates ->
            coordinates.add(coordinates[0])
        }
        return latLongs
    }

    /*
        At the first time, we get the devices from the API
        then save their points both on H3 and H7 so they can be served immediately afterwards

        Send the List of points back so we can show them on the explorer
    */
    override suspend fun getPointsFromPublicDevices(zoom: Double): Either<Failure, List<PolygonAnnotationOptions>> {
        val pointsToReturn: MutableList<PolygonAnnotationOptions>

        if (zoom <= ZOOM_LEVEL_CHANGE_HEX) {
            if (pointsHex3.isNullOrEmpty()) {
                deviceRepository.getPublicDevices()
                    .mapLeft {
                        return Either.Left(it)
                    }
                    .map {
                        saveDevicesPoints(it)
                    }
            }
            pointsToReturn = pointsHex3
        } else {
            // We start with H3 so pointsHex7 should be initialized and filled by now
            pointsToReturn = pointsHex7
        }

        return Either.Right(pointsToReturn)
    }

    // Get the center of Hex3 of a device. Used for zooming in when clicked.
    override fun getCenterOfHex3(deviceWithResolution: DeviceWithResolution?): Point? {
        deviceWithResolution?.device?.attributes?.hex3?.center?.let { center ->
            return Point.fromLngLat(center.lon, center.lat)
        }

        return null
    }

    override fun deviceWithResToJson(device: PublicDevice, resolution: Int): JsonElement {
        return gson.toJsonTree(DeviceWithResolution(device, resolution))
    }

    // Save the points of the devices so we can serve them immediately when needed
    override suspend fun saveDevicesPoints(devices: List<PublicDevice>) {
        if (devices.isNullOrEmpty()) {
            Timber.d("No devices found. Skipping saving their points.")
            return
        }

        devices.forEach { device ->
            if (device.attributes?.hex3?.polygon != null && !isHex3Used(device)) {
                val polygonAnnotationOptions: PolygonAnnotationOptions = PolygonAnnotationOptions()
                    .withFillColor(resourcesHelper.getColor(R.color.hexFillColor))
                    .withFillOpacity(FILL_OPACITY_HEXAGONS)
                    .withFillOutlineColor(resourcesHelper.getColor(R.color.hexFillOutlineColor))
                    .withData(deviceWithResToJson(device, H3_RESOLUTION))
                    .withPoints(polygonPointsToLatLng(device.attributes.hex3.polygon))

                device.attributes.hex3.let { currentH3Hexes.add(it.index) }
                pointsHex3.add(polygonAnnotationOptions)
            }

            if (device.attributes?.hex7?.polygon != null) {
                val polygonAnnotationOptions: PolygonAnnotationOptions = PolygonAnnotationOptions()
                    .withFillColor(resourcesHelper.getColor(R.color.hexFillColor))
                    .withFillOpacity(FILL_OPACITY_HEXAGONS)
                    .withFillOutlineColor(resourcesHelper.getColor(R.color.hexFillOutlineColor))
                    .withData(deviceWithResToJson(device, H7_RESOLUTION))
                    .withPoints(polygonPointsToLatLng(device.attributes.hex7.polygon))

                pointsHex7.add(polygonAnnotationOptions)
            }
        }
    }

    private fun isHex3Used(device: PublicDevice): Boolean {
        return currentH3Hexes.contains(device.attributes?.hex3?.index)
    }
}

