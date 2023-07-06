package com.weatherxm.data.repository

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.handleErrorWith
import arrow.core.left
import arrow.core.right
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
            .flatMap { b ->
                b.takeIf { isDateComplete(date, it) }?.right()
                    ?: DataError.DatabaseMissError.left<Failure>()
            }
            .onLeft {
                Timber.d("No data in db for $date")
            }
    }

    private suspend fun getHistoryFromNetwork(
        deviceId: String,
        date: LocalDate
    ): Either<Failure, List<HourlyWeather>> {
        Timber.d("Get device history from network")
        return networkSource.getWeatherHistory(deviceId, date, date)
            .onRight {
                if (it.isNotEmpty()) {
                    Timber.d("Save data in db ${it.first().timestamp..it.last().timestamp}")
                    databaseSource.setWeatherHistory(deviceId, it)
                } else {
                    Timber.d("No data to save in db!")
                }
            }
    }

    /**
     * Returns true if a data-day is complete. In this first implementation, since we know
     * we are requesting hourly aggregations, it's safe to assume we need at least (or exactly)
     * 24 hourly items and definitely values within 2 hours from the  the start/end of day.
     *
     * TODO This could be improved with a more sophisticated timeseries continuity check.
     */
    @SuppressWarnings("ReturnCount")
    private fun isDateComplete(
        date: LocalDate,
        data: List<HourlyWeather>,
        startOfDayMaxOffsetHours: Long = 2L,
        endOfDayMaxOffsetHours: Long = 2L,
        minHourlyLength: Long = 24L
    ): Boolean {
        if (data.isEmpty()) {
            Timber.d("History for $date is empty")
            return false
        }

        if (data.size < minHourlyLength) {
            Timber.d("History for $date is too short (size=${data.size})")
            return false
        }

        val tz = data.firstNotNullOfOrNull { it.timestamp.zone } ?: return false

        // Check if the first element is within startOfDayMaxOffsetHours hours from the start of day
        val startOfDay = date.atStartOfDay(tz)
        if (data.first().timestamp.isAfter(startOfDay.plusHours(startOfDayMaxOffsetHours))) {
            Timber.d("First hourly value for $date is too late)")
            return false
        }

        // Check if the last element is within endOfDayMaxOffsetHours hours from the end of day
        val endOfDay = date.plusDays(1).atStartOfDay(tz).minusNanos(1)
        if (data.last().timestamp.isBefore(endOfDay.minusHours(endOfDayMaxOffsetHours))) {
            Timber.d("First hourly value for $date is too late)")
            return false
        }

        return true
    }
}
