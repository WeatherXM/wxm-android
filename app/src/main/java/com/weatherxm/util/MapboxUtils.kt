package com.weatherxm.util

import com.google.gson.JsonObject
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotation
import com.mapbox.search.result.SearchSuggestion
import com.weatherxm.ui.explorer.UIHex
import com.weatherxm.ui.explorer.UIHexJsonAdapter
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
}
