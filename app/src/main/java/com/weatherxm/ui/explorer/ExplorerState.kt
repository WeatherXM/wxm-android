package com.weatherxm.ui.explorer

import androidx.annotation.Keep
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotationOptions
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.weatherxm.data.Device

data class ExplorerState(
    var polygonPoints: List<PolygonAnnotationOptions>?,
    // Not sure if needed
    var currentDevices: List<Device>?,
    // Not sure if needed
    var errorMessage: String?
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
