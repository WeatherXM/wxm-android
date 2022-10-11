package com.weatherxm.data.repository

import arrow.core.Either
import arrow.core.filterOrElse
import arrow.core.handleErrorWith
import com.weatherxm.data.DataError
import com.weatherxm.data.Failure
import com.weatherxm.data.HourlyWeather
import com.weatherxm.data.datasource.DatabaseWeatherHistoryDataSource
import com.weatherxm.data.datasource.NetworkWeatherHistoryDataSource
import org.koin.core.component.KoinComponent
import timber.log.Timber
import java.time.LocalDate

class WeatherHistoryRepositoryImpl(
    private val networkSource: NetworkWeatherHistoryDataSource,
    private val databaseSource: DatabaseWeatherHistoryDataSource
) : WeatherHistoryRepository, KoinComponent {

    override suspend fun getHourlyWeatherHistory(
        deviceId: String,
        date: LocalDate,
        forceUpdate: Boolean
    ): Either<Failure, List<HourlyWeather>> {
        return if (forceUpdate) {
            Timber.d("Forced update. Skipping db.")
            getHistoryFromNetwork(deviceId, date)
        } else {
            Timber.d("Non-forced update. Trying db first.")
            getHistoryFromDatabase(deviceId, date)
                .handleErrorWith {
                    getHistoryFromNetwork(deviceId, date)
                }
        }
    }

    private suspend fun getHistoryFromDatabase(
        deviceId: String,
        date: LocalDate
    ): Either<Failure, List<HourlyWeather>> {
        return databaseSource.getWeatherHistory(deviceId, date, date)
            .map {
                Timber.d("Got device history from db")
                it.filter { hourly ->
                    hourly.timestamp.toLocalDate().toEpochDay() == date.toEpochDay()
                }
            }
            .filterOrElse({ it.isNotEmpty() }, { DataError.DatabaseMissError })
            .tapLeft {
                Timber.d("No data in db for $date")
            }
    }

    private suspend fun getHistoryFromNetwork(
        deviceId: String,
        date: LocalDate
    ): Either<Failure, List<HourlyWeather>> {
        Timber.d("Get device history from network")
        return networkSource.getWeatherHistory(deviceId, date, date)
            .tap {
                if (it.isNotEmpty()) {
                    Timber.d("Save data in db ${it.first().timestamp..it.last().timestamp}")
                    databaseSource.setWeatherHistory(deviceId, it)
                } else {
                    Timber.d("No data to save in db!")
                }
            }
    }
}
