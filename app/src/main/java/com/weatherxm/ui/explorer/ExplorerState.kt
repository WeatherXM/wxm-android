package com.weatherxm.ui.explorer

import androidx.annotation.Keep
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotationOptions
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

data class ExplorerState(
    var polygonPoints: List<PolygonAnnotationOptions>?,
)

@Keep
@JsonClass(generateAdapter = true)
data class HexWithResolution(
    var index: String,
    var lat: Double,
    var lon: Double,
    @Json(name = "current_resolution")
    var currentResolution: Int
)
