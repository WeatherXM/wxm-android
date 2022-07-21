package com.weatherxm.util

import com.google.gson.JsonObject
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotation
import com.weatherxm.ui.UIHex
import com.weatherxm.ui.UIHexJsonAdapter
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

fun PolygonAnnotation.getCustomData(): JsonObject {
    return this.getJsonObjectCopy().getAsJsonObject("custom_data")
}

object MapboxUtils : KoinComponent {
    private val adapter: UIHexJsonAdapter by inject()

    fun getCustomData(polygonAnnotation: PolygonAnnotation): UIHex? {
        val data = polygonAnnotation.getCustomData()
        return adapter.fromJson(data.toString())
    }
}
