package com.weatherxm.ui.widgets

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class WidgetType : Parcelable {
    CURRENT_WEATHER,
    CURRENT_WEATHER_TILE,
    CURRENT_WEATHER_DETAILED,
}
