package com.weatherxm.usecases

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.collection.ArrayMap
import androidx.collection.arrayMapOf
import arrow.core.Either
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.weatherxm.R
import com.weatherxm.data.Device
import com.weatherxm.data.Failure
import com.weatherxm.data.HourlyWeather
import com.weatherxm.data.repository.WeatherHistoryRepository
import com.weatherxm.ui.BarChartData
import com.weatherxm.ui.HistoryCharts
import com.weatherxm.ui.LineChartData
import com.weatherxm.util.DateTimeHelper.getFormattedDate
import com.weatherxm.util.DateTimeHelper.getHourMinutesFromISO
import com.weatherxm.util.ResourcesHelper
import com.weatherxm.util.UnitConverter
import com.weatherxm.util.Weather

interface HistoryUseCase {
    suspend fun getWeatherHistory(
        device: Device,
        fromDate: String,
        toDate: String,
        context: Context
    ): Either<Failure, List<HistoryCharts>>
}

class HistoryUseCaseImpl(
    private val weatherHistoryRepository: WeatherHistoryRepository,
    private val resHelper: ResourcesHelper
) : HistoryUseCase {

    /*
        Return either a HistoryCharts object with all the entries for the chart of each data
        Either a Failure object
    */
    override suspend fun getWeatherHistory(
        device: Device,
        fromDate: String,
        toDate: String,
        context: Context
    ): Either<Failure, List<HistoryCharts>> {
        return weatherHistoryRepository.getHourlyWeatherHistory(device.id, fromDate, toDate)
            .map { deviceHourlyHistory ->
                return Either.Right(createAllHourlyCharts(deviceHourlyHistory, context))
            }
    }

    private fun createTemperatureLineChartData(
        time: MutableList<String>,
        entries: MutableList<Entry>
    ): LineChartData {
        return LineChartData(
            resHelper.getString(R.string.temperature),
            R.color.temperature,
            Weather.getPreferredUnit(
                resHelper.getString(R.string.key_temperature_preference),
                resHelper.getString(R.string.temperature_celsius)
            ),
            timestamps = time,
            entries = entries
        )
    }

    private fun createPrecipitationLineChartData(
        time: MutableList<String>,
        entries: MutableList<Entry>
    ): LineChartData {
        return LineChartData(
            resHelper.getString(R.string.precipitation),
            R.color.precipIntensity,
            Weather.getPrecipitationPreferredUnit(false),
            timestamps = time,
            entries = entries
        )
    }

    private fun createWindSpeedLineChartData(
        time: MutableList<String>,
        entries: MutableList<Entry>
    ): LineChartData {
        return LineChartData(
            resHelper.getString(R.string.wind_speed),
            R.color.windSpeed,
            Weather.getPreferredUnit(
                resHelper.getString(R.string.key_wind_speed_preference),
                resHelper.getString(R.string.wind_speed_ms)
            ),
            timestamps = time,
            entries = entries
        )
    }

    private fun createWindGustLineChartData(
        time: MutableList<String>,
        entries: MutableList<Entry>
    ): LineChartData {
        return LineChartData(
            resHelper.getString(R.string.wind_gust),
            R.color.windGust,
            Weather.getPreferredUnit(
                resHelper.getString(R.string.key_wind_speed_preference),
                resHelper.getString(R.string.wind_speed_ms)
            ),
            timestamps = time,
            entries = entries
        )
    }

    private fun createWindDirectionLineChartData(
        time: MutableList<String>,
        entries: MutableList<Entry>
    ): LineChartData {
        return LineChartData(
            resHelper.getString(R.string.wind_direction),
            R.color.windSpeed,
            Weather.getPreferredUnit(
                resHelper.getString(R.string.key_wind_direction_preference),
                resHelper.getString(R.string.wind_direction_cardinal)
            ),
            timestamps = time,
            entries = entries
        )
    }

    private fun createHumidityLineChartData(
        time: MutableList<String>,
        entries: MutableList<Entry>
    ): LineChartData {
        return LineChartData(
            resHelper.getString(R.string.humidity),
            R.color.humidity,
            resHelper.getString(R.string.percent),
            timestamps = time,
            entries = entries
        )
    }

    private fun createPressureLineChartData(
        time: MutableList<String>,
        entries: MutableList<Entry>
    ): LineChartData {
        return LineChartData(
            resHelper.getString(R.string.pressure),
            R.color.pressure,
            Weather.getPreferredUnit(
                resHelper.getString(R.string.key_pressure_preference),
                resHelper.getString(R.string.pressure_hpa)
            ),
            timestamps = time,
            entries = entries
        )
    }

    private fun createUvIndexBarChartData(
        time: MutableList<String>,
        entries: MutableList<BarEntry>
    ): BarChartData {
        return BarChartData(
            resHelper.getString(R.string.uv_index),
            R.color.uvIndex,
            resHelper.getString(R.string.uv_index_unit),
            timestamps = time,
            entries = entries
        )
    }

    private fun createHourlyCharts(
        context: Context,
        date: String,
        hourlyWeatherData: List<HourlyWeather>
    ): HistoryCharts {
        var counter = 0F

        val temperatureEntries = mutableListOf<Entry>()
        val precipEntries = mutableListOf<Entry>()
        val windSpeedEntries = mutableListOf<Entry>()
        val windGustEntries = mutableListOf<Entry>()
        val windDirectionEntries = mutableListOf<Entry>()
        val humidityEntries = mutableListOf<Entry>()
        val pressureEntries = mutableListOf<Entry>()
        val uvIndexEntries = mutableListOf<BarEntry>()
        val time = mutableListOf<String>()

        hourlyWeatherData.forEach { hourlyWeather ->

            hourlyWeather.timestamp.let { timestampNonNull ->
                // Set showMinutes12Format as false
                // on hourly data they don't matter and they cause UI issues
                time.add(getHourMinutesFromISO(context, timestampNonNull, false))

                hourlyWeather.temperature?.let {
                    temperatureEntries.add(Entry(counter, Weather.convertTemp(it, 1) as Float))
                }

                hourlyWeather.precipitation?.let {
                    precipEntries.add(Entry(counter, Weather.convertPrecipitation(it) as Float))
                }

                // Get the wind speed and direction formatted
                val windSpeed = Weather.convertWindSpeed(hourlyWeather.windSpeed)
                var windDirection: Drawable? = null
                if (hourlyWeather.windDirection != null) {
                    val index = UnitConverter.getIndexOfCardinal(hourlyWeather.windDirection)
                    windDirection = resHelper.getWindDirectionDrawable(index)
                    windDirectionEntries.add(Entry(counter, hourlyWeather.windDirection.toFloat()))
                }
                if (windSpeed != null && windDirection != null && windSpeed.toFloat() > 0) {
                    windSpeedEntries.add(Entry(counter, windSpeed.toFloat(), windDirection))
                } else if (windSpeed != null) {
                    windSpeedEntries.add(Entry(counter, windSpeed.toFloat()))
                }

                val windGust = Weather.convertWindSpeed(hourlyWeather.windGust)
                if (windGust != null) {
                    windGustEntries.add(Entry(counter, windGust.toFloat()))
                }

                hourlyWeather.pressure?.let {
                    pressureEntries.add(Entry(counter, Weather.convertPressure(it) as Float))
                }

                hourlyWeather.humidity?.let {
                    humidityEntries.add(Entry(counter, it.toFloat()))
                }

                hourlyWeather.uvIndex?.let {
                    uvIndexEntries.add(BarEntry(counter, it.toFloat()))
                }
            }

            counter++
        }

        return HistoryCharts(
            date,
            createTemperatureLineChartData(time, temperatureEntries),
            createPrecipitationLineChartData(time, precipEntries),
            createWindSpeedLineChartData(time, windSpeedEntries),
            createWindGustLineChartData(time, windGustEntries),
            createWindDirectionLineChartData(time, windDirectionEntries),
            createHumidityLineChartData(time, humidityEntries),
            createPressureLineChartData(time, pressureEntries),
            createUvIndexBarChartData(time, uvIndexEntries)
        )
    }

    private fun createAllHourlyCharts(
        data: List<HourlyWeather>,
        context: Context
    ): List<HistoryCharts> {

        val datesAndData: ArrayMap<String, MutableList<HourlyWeather>> = arrayMapOf()

        data.forEach {
            val date = getFormattedDate(it.timestamp)
            if (datesAndData.contains(date)) {
                datesAndData[date]?.add(it)
            } else {
                datesAndData[date] = mutableListOf(it)
            }
        }

        val charts = datesAndData.map {
            createHourlyCharts(context, it.key, it.value)
        }

        return charts
    }
}
