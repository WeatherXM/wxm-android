package com.weatherxm.util

import com.google.gson.Gson
import com.mapbox.api.staticmap.v1.MapboxStaticMap
import com.mapbox.api.staticmap.v1.StaticMapCriteria
import com.mapbox.api.staticmap.v1.models.StaticMarkerAnnotation
import com.mapbox.api.staticmap.v1.models.StaticPolylineAnnotation
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.maps.extension.style.expressions.dsl.generated.get
import com.mapbox.maps.extension.style.expressions.dsl.generated.switchCase
import com.mapbox.maps.extension.style.layers.generated.fillLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotation
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotationOptions
import com.mapbox.search.result.SearchSuggestion
import com.weatherxm.R
import com.weatherxm.data.models.Hex
import com.weatherxm.data.models.Location
import com.weatherxm.data.models.PublicHex
import com.weatherxm.ui.common.CapacityLayerOnSetLocation
import com.weatherxm.ui.home.explorer.ExplorerViewModel.Companion.FILL_OPACITY_HEXAGONS
import com.weatherxm.ui.home.explorer.ExplorerViewModel.Companion.SHOW_STATION_COUNT_ZOOM_LEVEL
import com.weatherxm.ui.home.explorer.MapLayer
import com.weatherxm.ui.home.explorer.UICell
import com.weatherxm.ui.home.explorer.UICellJsonAdapter
import com.weatherxm.util.Rewards.getRewardScoreColor
import okhttp3.HttpUrl
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object MapboxUtils : KoinComponent {
    private const val HEX_OPACITY_FOR_SET_LOCATION = 0.2

    private val adapter: UICellJsonAdapter by inject()
    private val resources: Resources by inject()
    private val gson: Gson by inject()

    fun getCustomData(polygonAnnotation: PolygonAnnotation): UICell? {
        val data = polygonAnnotation.getJsonObjectCopy().getAsJsonObject("custom_data")
        return adapter.fromJson(data.toString())
    }

    fun parseSearchSuggestion(searchSuggestion: SearchSuggestion): String {
        var parsedAddress = searchSuggestion.name
        val searchAddress = searchSuggestion.address
        searchAddress?.street?.let {
            parsedAddress += ", $it"
        }
        searchAddress?.place?.let {
            parsedAddress += ", $it"
        }
        searchAddress?.region?.let {
            parsedAddress += ", $it"
        }
        searchAddress?.country?.let {
            parsedAddress += ", $it"
        }
        searchAddress?.postcode?.let {
            parsedAddress += ", $it"
        }
        return parsedAddress
    }

    @Suppress("MagicNumber")
    fun getMinimap(width: Int, userLocation: Location?, hex: Hex?): HttpUrl? {
        return hex?.let {
            val hexPoints = hex.polygon.map {
                Point.fromLngLat(it.lon, it.lat)
            }
            val staticHex = StaticPolylineAnnotation.builder()
                .polyline(PolylineUtils.encode(hexPoints, 5))
                .fillColor("3388ff")
                .fillOpacity(FILL_OPACITY_HEXAGONS.toFloat())
                .strokeColor("FFFFFF")
                .strokeWidth(0.0)
                .build()

            with(MapboxStaticMap.builder()) {
                accessToken(resources.getString(R.string.mapbox_access_token))
                styleId(StaticMapCriteria.DARK_STYLE)
                cameraPoint(Point.fromLngLat(hex.center.lon, hex.center.lat))
                cameraZoom(11.0)
                width(width)
                height(200)
                staticPolylineAnnotations(listOf(staticHex))
                retina(true)

                userLocation?.let {
                    val marker = StaticMarkerAnnotation.builder()
                        .color("0A3FAD")
                        .lnglat(Point.fromLngLat(userLocation.lon, userLocation.lat))
                        .build()
                    staticMarkerAnnotations(listOf(marker))
                }
                build()
            }.url()
        }
    }

    fun List<PublicHex>.toPolygonAnnotationOptions(
        layer: MapLayer
    ): List<PolygonAnnotationOptions> {
        return map {
            val fillColor = when (layer) {
                MapLayer.DENSITY -> R.color.hex_fill_color
                MapLayer.DATA_QUALITY -> getRewardScoreColor(it.avgDataQuality)
            }
            PolygonAnnotationOptions()
                .withFillColor(resources.getColor(fillColor))
                .withFillOpacity(FILL_OPACITY_HEXAGONS)
                .withFillOutlineColor(resources.getColor(R.color.white))
                .withData(gson.toJsonTree(UICell(it.index, it.center)))
                .withPoints(listOf(polygonPointsToLatLng(it.polygon)))
        }
    }

    fun List<PublicHex>.toDeviceCountPoints(): List<PointAnnotationOptions> {
        return mapNotNull { hex ->
            hex.deviceCount?.takeIf { it > 0 }?.let {
                PointAnnotationOptions()
                    .withPoint(Point.fromLngLat(hex.center.lon, hex.center.lat))
                    .withTextField(it.toString())
                    .withTextColor(resources.getColor(R.color.dark_text))
            }
        }
    }

    fun List<PublicHex>.toCapacityPoints(): List<PointAnnotationOptions> {
        return mapNotNull { hex ->
            hex.capacity?.takeIf { it > 0 }?.let {
                PointAnnotationOptions()
                    .withPoint(Point.fromLngLat(hex.center.lon, hex.center.lat))
                    .withTextField(it.toString())
                    .withTextColor(resources.getColor(R.color.dark_text))
            }
        }
    }

    fun polygonPointsToLatLng(pointsOfPolygon: List<Location>): MutableList<Point> {
        val latLongs = pointsOfPolygon.map { coordinates ->
            Point.fromLngLat(coordinates.lon, coordinates.lat)
        }.toMutableList()

        // Custom/Temporary fix for: https://github.com/mapbox/mapbox-maps-android/issues/733
        latLongs.add(latLongs[0])
        return latLongs
    }

    fun createCapacityLayer(hexes: List<PublicHex>): CapacityLayerOnSetLocation {
        val features = hexes.map {
            val isOverCapacity =
                it.capacity != null && it.deviceCount != null && it.deviceCount >= it.capacity

            Feature.fromGeometry(
                Polygon.fromLngLats(
                    listOf(polygonPointsToLatLng(it.polygon))
                )
            ).apply {
                addBooleanProperty("is_over_capacity", isOverCapacity)
                addStringProperty("capacity_number", it.capacity.toString())
            }
        }

        val source = geoJsonSource("capacity-source") {
            featureCollection(FeatureCollection.fromFeatures(features))
        }

        val fillLayer = fillLayer("capacity-fill-layer", "capacity-source") {
            fillOpacity(HEX_OPACITY_FOR_SET_LOCATION)
            fillColor(
                switchCase {
                    get {
                        literal("is_over_capacity")
                    }
                    color(resources.getColor(R.color.error))
                    color(resources.getColor(R.color.colorPrimary))
                }
            )
        }

        val lineLayer = lineLayer("capacity-outline-layer", "capacity-source") {
            lineWidth(2.0)
            lineColor(
                switchCase {
                    get {
                        literal("is_over_capacity")
                    }
                    color(resources.getColor(R.color.error))
                    color(resources.getColor(R.color.colorPrimary))
                }
            )
        }

        val textLayer = symbolLayer("capacity-text-layer", "capacity-source") {
            textField(
                get {
                    literal("capacity_number")
                }
            )
            textSize(18.0)
            textColor(resources.getColor(R.color.dark_text))
            minZoom(SHOW_STATION_COUNT_ZOOM_LEVEL)
            textAllowOverlap(true)
            textIgnorePlacement(true)
            iconAllowOverlap(true)
            iconIgnorePlacement(true)
        }

        return CapacityLayerOnSetLocation(source, fillLayer, lineLayer, textLayer)
    }
}
