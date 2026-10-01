package com.weatherxm.usecases

import com.weatherxm.data.models.HourlyWeather
import com.weatherxm.ui.common.Charts
import java.time.LocalDate

interface ChartsUseCase {
    fun createHourlyCharts(
        date: LocalDate,
        hourlyWeatherData: List<HourlyWeather>
    ): Charts
}
