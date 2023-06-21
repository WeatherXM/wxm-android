package com.weatherxm.ui.explorer

import androidx.annotation.Keep
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotationOptions
import com.squareup.moshi.JsonClass
import com.weatherxm.data.Location

@Keep
@JsonClass(generateAdapter = true)
data class ExplorerData(
    var geoJsonSource: GeoJsonSource,
    var polygonPoints: List<PolygonAnnotationOptions>,
)

@Keep
@JsonClass(generateAdapter = true)
data class UIHex(
    var index: String,
    var center: Location
)

@Keep
@JsonClass(generateAdapter = true)
data class ExplorerCamera(
    var zoom: Double,
    var center: Point
)
