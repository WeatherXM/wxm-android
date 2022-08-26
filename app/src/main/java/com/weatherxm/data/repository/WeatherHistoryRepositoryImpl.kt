package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.HourlyWeather
import com.weatherxm.data.datasource.DatabaseWeatherHistoryDataSource
import com.weatherxm.data.datasource.NetworkWeatherHistoryDataSource
import com.weatherxm.util.DateTimeHelper.dateToLocalDate
import com.weatherxm.util.DateTimeHelper.getLocalDate
import org.koin.core.component.KoinComponent
import java.time.ZonedDateTime

class WeatherHistoryRepositoryImpl(
    private val networkSource: NetworkWeatherHistoryDataSource,
    private val databaseSource: DatabaseWeatherHistoryDataSource
) : WeatherHistoryRepository, KoinComponent {

    override suspend fun getHourlyWeatherHistory(
        deviceId: String,
        fromDate: String,
        toDate: String
    ): Either<Failure, List<HourlyWeather>> {

        val savedData =
            databaseSource.getWeatherHistory(deviceId, fromDate, toDate, null)

        /*
        * OPTIMIZATION:
        * Get the latest saved day. If it is after the fromDate we want to fetch data from
        * then change that fromDate to the next day after the last saved day.
         */
        var lastTimestampSaved: String? = null
        savedData.map {
            lastTimestampSaved = if (it.isNotEmpty()) it.last().timestamp else null
        }.mapLeft {
            null
        }

        val newFromDate = if (
            lastTimestampSaved != null
            && getLocalDate(lastTimestampSaved).isAfter(dateToLocalDate(fromDate))
        ) {
            getLocalDate(lastTimestampSaved).plusDays(1).toString()
        } else {
            fromDate
        }

        return networkSource.getWeatherHistory(
            deviceId,
            newFromDate,
            toDate,
            "daily"
        ).map { networkData ->
            // Save the "new" days (skipping the current day/toDate) in db asynchronously
            networkData
                .filter {
                    getLocalDate(it.timestamp) != ZonedDateTime.now().toLocalDate()
                }
                .apply {
                    if (isNotEmpty()) {
                        databaseSource.setWeatherHistory(deviceId, this)
                    }
                }

            // Return a combination of saved and new data
            val dataToReturn = mutableListOf<HourlyWeather>()

            savedData.map {
                dataToReturn.addAll(it)
            }
            dataToReturn.addAll(networkData)

            dataToReturn.toList()
        }
    }
}
