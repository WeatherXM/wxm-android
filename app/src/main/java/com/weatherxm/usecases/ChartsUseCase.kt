package com.weatherxm.usecases

import com.weatherxm.data.models.HourlyWeather
import com.weatherxm.ui.common.Charts
import java.time.Duration
import java.time.LocalDate

interface ChartsUseCase {
    fun createHourlyCharts(
        date: LocalDate,
        hourlyWeatherData: List<HourlyWeather>,
        chartStep: Duration = Duration.ofHours(1)
    ): Charts
}
