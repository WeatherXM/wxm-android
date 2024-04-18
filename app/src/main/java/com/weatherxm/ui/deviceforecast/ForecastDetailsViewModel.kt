package com.weatherxm.ui.deviceforecast

import androidx.lifecycle.ViewModel
import com.weatherxm.data.HourlyWeather
import com.weatherxm.ui.common.Charts
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.common.UIForecast
import com.weatherxm.ui.common.UIForecastDay
import com.weatherxm.usecases.ChartsUseCase
import timber.log.Timber

class ForecastDetailsViewModel(
    val device: UIDevice,
    val forecast: UIForecast,
    private val chartsUseCase: ChartsUseCase,
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
     * 2. Show first the 07:00am hour or
     * 3. Show first the first available hour
     */
    @Suppress("MagicNumber")
    fun getDefaultHourPosition(hourlies: List<HourlyWeather>): Int {
        return hourlies.indexOf(
            hourlies.firstOrNull {
                it.timestamp.hour == 7
            } ?: hourlies[0]
        )
    }

    fun getCharts(forecastDay: UIForecastDay): Charts {
        Timber.d("Returning forecast charts for [${forecastDay.date}]")
        return chartsUseCase.createHourlyCharts(
            forecastDay.date, forecastDay.hourlyWeather ?: mutableListOf()
        )
    }
}
