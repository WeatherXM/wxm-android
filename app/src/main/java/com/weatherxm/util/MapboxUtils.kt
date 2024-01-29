package com.weatherxm.util

import com.mapbox.api.staticmap.v1.MapboxStaticMap
import com.mapbox.api.staticmap.v1.StaticMapCriteria
import com.mapbox.api.staticmap.v1.models.StaticMarkerAnnotation
import com.mapbox.api.staticmap.v1.models.StaticPolylineAnnotation
import com.mapbox.geojson.Point
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotation
import com.mapbox.search.result.SearchSuggestion
import com.weatherxm.R
import com.weatherxm.data.Hex
import com.weatherxm.data.Location
import com.weatherxm.ui.explorer.ExplorerViewModel.Companion.FILL_OPACITY_HEXAGONS
import com.weatherxm.ui.explorer.UICell
import com.weatherxm.ui.explorer.UICellJsonAdapter
import okhttp3.HttpUrl
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object MapboxUtils : KoinComponent {
    private val adapter: UICellJsonAdapter by inject()
    private val resources: Resources by inject()

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
}
