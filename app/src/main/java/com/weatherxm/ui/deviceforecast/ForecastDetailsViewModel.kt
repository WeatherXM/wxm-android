package com.weatherxm.ui.deviceforecast

import androidx.lifecycle.ViewModel
import com.weatherxm.data.HourlyWeather
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.UIForecast
import com.weatherxm.ui.common.UIForecastDay

class ForecastDetailsViewModel(
    val device: UIDevice,
    val forecast: UIForecast,
) : ViewModel() {
    fun getSelectedDayPosition(selectedDate: UIForecastDay?, selectedHour: HourlyWeather?): Int {
        return if (selectedDate != null) {
            forecast.forecastDays.indexOf(selectedDate)
        } else if (selectedHour != null) {
            forecast.forecastDays.indexOfFirst {
                it.date == selectedHour.timestamp.toLocalDate()
            }
        } else {
            0
        }
    }

    /**
     * 1. Show first the selected hour or
     * 2. Show first the 07:00am hour or
     * 3. Show first the first available hour
     */
    @Suppress("MagicNumber")
    fun getSelectedHourPosition(hourlies: List<HourlyWeather>, selectedHour: HourlyWeather?): Int {
        return if (selectedHour != null) {
            hourlies.indexOf(selectedHour)
        } else {
            hourlies.indexOf(
                hourlies.firstOrNull {
                    it.timestamp.hour == 7
                } ?: hourlies[0]
            )
        }
    }
}
