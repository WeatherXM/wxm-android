package com.weatherxm.usecases

import arrow.core.Either
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.mapbox.geojson.Point
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotationOptions
import com.weatherxm.R
import com.weatherxm.data.Device
import com.weatherxm.data.Failure
import com.weatherxm.data.Location
import com.weatherxm.data.repository.DeviceRepository
import com.weatherxm.ui.explorer.ExplorerViewModel.Companion.FILL_OPACITY_HEXAGONS
import com.weatherxm.ui.explorer.ExplorerViewModel.Companion.H3_RESOLUTION
import com.weatherxm.ui.explorer.ExplorerViewModel.Companion.H7_RESOLUTION
import com.weatherxm.ui.explorer.ExplorerViewModel.Companion.ZOOM_LEVEL_CHANGE_HEX
import com.weatherxm.ui.explorer.HexWithResolution
import com.weatherxm.util.ResourcesHelper
import timber.log.Timber

interface ExplorerUseCase {
    fun polygonPointsToLatLng(pointsOfPolygon: Array<Location>): List<MutableList<Point>>
    fun getPointsFromPublicDevices(
        zoom: Double
    ): List<PolygonAnnotationOptions>

    fun getCenterOfHex3AsPoint(hexCenterWithResolution: HexWithResolution?): Point?
    fun hexWithResToJson(index: String, center: Location, resolution: Int): JsonElement
    fun getDevicesOfH7(hexIndex: String?): MutableList<Device>?
    suspend fun getPublicDevices(forceRefresh: Boolean = false): Either<Failure, List<Device>>
    suspend fun saveDevicesPointsH3(devices: List<Device>)
    suspend fun saveDevicesPointsH7(devices: List<Device>)
}

class ExplorerUseCaseImpl(
    private val deviceRepository: DeviceRepository,
    private val gson: Gson,
    private val resHelper: ResourcesHelper
) : ExplorerUseCase {

    // Points to paint
    private var pointsHex7: MutableList<PolygonAnnotationOptions> = mutableListOf()
    private var pointsHex3: MutableList<PolygonAnnotationOptions> = mutableListOf()

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
    * On the first or a forceRefresh run (when pointsHex3 and pointsHex7 are or should get empty)
    * save their points both on H3 and H7 so they can be served immediately afterwards
    */
    override suspend fun getPublicDevices(forceRefresh: Boolean): Either<Failure, List<Device>> {
        if (forceRefresh) {
            pointsHex3.clear()
            pointsHex7.clear()
        }
        return deviceRepository.getPublicDevices(forceRefresh).tap {
            if (pointsHex3.isEmpty()) {
                saveDevicesPointsH3(it)
            }
            if (pointsHex7.isEmpty()) {
                saveDevicesPointsH7(it)
            }
        }
    }

    override fun getPointsFromPublicDevices(zoom: Double): List<PolygonAnnotationOptions> {
        return if (zoom <= ZOOM_LEVEL_CHANGE_HEX) {
            pointsHex3
        } else {
            pointsHex7
        }
    }

    // Get the center of Hex3 of a device. Used for zooming in when clicked.
    override fun getCenterOfHex3AsPoint(hexCenterWithResolution: HexWithResolution?): Point? {
        hexCenterWithResolution?.let {
            return Point.fromLngLat(it.lon, it.lat)
        }

        return null
    }

    override fun hexWithResToJson(index: String, center: Location, resolution: Int): JsonElement {
        return gson.toJsonTree(HexWithResolution(index, center.lat, center.lon, resolution))
    }

    // Save the points of the devices in H3 so we can serve them immediately when needed
    override suspend fun saveDevicesPointsH3(devices: List<Device>) {
        if (devices.isEmpty()) {
            Timber.d("No devices found. Skipping saving their points.")
            return
        }

        // Needed lists to not save duplicate points of H3 so they are being painted only once
        val alreadySetH3 = mutableListOf<String>()
        devices.forEach { device ->
            device.attributes?.hex3?.let {
                if (!alreadySetH3.contains(it.index)) {
                    val polygonAnnotationOptions: PolygonAnnotationOptions =
                        PolygonAnnotationOptions()
                            .withFillColor(resHelper.getColor(R.color.hexFillColor))
                            .withFillOpacity(FILL_OPACITY_HEXAGONS)
                            .withFillOutlineColor(resHelper.getColor(R.color.hexFillOutlineColor))
                            .withData(hexWithResToJson(it.index, it.center, H3_RESOLUTION))
                            .withPoints(polygonPointsToLatLng(it.polygon))

                    pointsHex3.add(polygonAnnotationOptions)
                    alreadySetH3.add(it.index)
                }
            }
        }
    }

    // Save the points of the devices in H7 so we can serve them immediately when needed
    override suspend fun saveDevicesPointsH7(devices: List<Device>) {
        if (devices.isEmpty()) {
            Timber.d("No devices found. Skipping saving their points.")
            return
        }

        // Needed lists to not save duplicate points of H7 so they are being painted only once
        val alreadySetH7 = mutableListOf<String>()
        devices.forEach { device ->
            device.attributes?.hex7?.let {
                if (!alreadySetH7.contains(it.index)) {
                    val polygonAnnotationOptions = PolygonAnnotationOptions()
                        .withFillColor(resHelper.getColor(R.color.hexFillColor))
                        .withFillOpacity(FILL_OPACITY_HEXAGONS)
                        .withFillOutlineColor(resHelper.getColor(R.color.hexFillOutlineColor))
                        .withData(hexWithResToJson(it.index, it.center, H7_RESOLUTION))
                        .withPoints(polygonPointsToLatLng(it.polygon))

                    pointsHex7.add(polygonAnnotationOptions)
                    alreadySetH7.add(it.index)
                }
            }
        }
    }

    override fun getDevicesOfH7(hexIndex: String?): MutableList<Device> {
        return deviceRepository.getDevicesOfH7(hexIndex)
    }
}

