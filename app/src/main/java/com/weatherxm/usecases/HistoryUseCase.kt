package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.HourlyWeather
import com.weatherxm.data.repository.WeatherHistoryRepository
import com.weatherxm.ui.common.UIDevice
import java.time.LocalDate

interface HistoryUseCase {
    suspend fun getWeatherHistory(
        device: UIDevice,
        date: LocalDate,
        forceUpdate: Boolean = false
    ): Either<Failure, List<HourlyWeather>>
}

class HistoryUseCaseImpl(private val repository: WeatherHistoryRepository) : HistoryUseCase {

    override suspend fun getWeatherHistory(
        device: UIDevice,
        date: LocalDate,
        forceUpdate: Boolean
    ): Either<Failure, List<HourlyWeather>> {
        return repository.getHourlyWeatherHistory(device.id, date, forceUpdate)
    }
}
