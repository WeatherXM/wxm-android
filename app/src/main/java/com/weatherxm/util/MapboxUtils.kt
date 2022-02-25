package com.weatherxm.util

import com.google.gson.JsonObject
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotation
import com.weatherxm.ui.explorer.HexWithResolution
import com.weatherxm.ui.explorer.HexWithResolutionJsonAdapter
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

fun PolygonAnnotation.getCustomData(): JsonObject {
    return this.getJsonObjectCopy().getAsJsonObject("custom_data")
}

object MapboxUtils : KoinComponent {
    private val adapter: HexWithResolutionJsonAdapter by inject()

    fun getCustomData(polygonAnnotation: PolygonAnnotation): HexWithResolution? {
        val data = polygonAnnotation.getCustomData()
        return adapter.fromJson(data.toString())
    }
}
