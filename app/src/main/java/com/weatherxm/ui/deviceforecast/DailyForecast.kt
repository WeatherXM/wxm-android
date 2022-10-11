package com.weatherxm.ui.deviceforecast

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
data class DailyForecast(
    var nameOfDay: String = "",
    var dateOfDay: String = "",
    var icon: String? = null,
    var minTemp: Float? = null,
    var maxTemp: Float? = null,
    var precipProbability: Int? = null
)
