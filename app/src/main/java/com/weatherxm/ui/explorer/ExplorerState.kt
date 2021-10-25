package com.weatherxm.ui.explorer

import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotationOptions
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.weatherxm.data.PublicDevice

data class ExplorerState(
    var polygonPoints: List<PolygonAnnotationOptions>?,
    // Not sure if needed
    var currentDevices: List<PublicDevice>?,
    // Not sure if needed
    var errorMessage: String?
)

@JsonClass(generateAdapter = true)
data class DeviceWithResolution(
    var device: PublicDevice,
    @Json(name = "current_resolution")
    var currentResolution: Int
)
