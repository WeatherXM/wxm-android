package com.weatherxm.usecases

import android.content.Context
import android.graphics.drawable.Drawable
import arrow.core.Either
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.weatherxm.R
import com.weatherxm.data.Device
import com.weatherxm.data.Failure
import com.weatherxm.data.HourlyWeather
import com.weatherxm.data.repository.WeatherHistoryRepository
import com.weatherxm.ui.devicehistory.BarChartData
import com.weatherxm.ui.devicehistory.HistoryCharts
import com.weatherxm.ui.devicehistory.LineChartData
import com.weatherxm.util.DateTimeHelper.getFormattedTime
import com.weatherxm.util.LocalDateTimeRange
import com.weatherxm.util.ResourcesHelper
import com.weatherxm.util.UnitConverter
import com.weatherxm.util.Weather
import java.time.LocalDate

interface HistoryUseCase {
    suspend fun getWeatherHistory(
        device: Device,
        date: LocalDate,
        forceUpdate: Boolean = false
    ): Either<Failure, HistoryCharts>
}

class HistoryUseCaseImpl(
    private val context: Context,
    private val weatherHistoryRepository: WeatherHistoryRepository,
    private val resHelper: ResourcesHelper
) : HistoryUseCase {

    override suspend fun getWeatherHistory(
        device: Device,
        date: LocalDate,
        forceUpdate: Boolean
    ): Either<Failure, HistoryCharts> {
        return weatherHistoryRepository.getHourlyWeatherHistory(device.id, date, forceUpdate)
            .map {
                createHourlyCharts(date, it)
            }
    }

    /**
     * Suppress long and Complex method warning by detekt because it is just a bunch of `.let`
     * and `.if` statements
     */
    @Suppress("LongMethod", "ComplexMethod")
    private fun createHourlyCharts(
        date: LocalDate,
        hourlyWeatherData: List<HourlyWeather>
    ): HistoryCharts {
        val temperatureEntries = mutableListOf<Entry>()
        val feelsLikeEntries = mutableListOf<Entry>()
        val precipEntries = mutableListOf<Entry>()
        val windSpeedEntries = mutableListOf<Entry>()
        val windGustEntries = mutableListOf<Entry>()
        val windDirectionEntries = mutableListOf<Entry>()
        val humidityEntries = mutableListOf<Entry>()
        val pressureEntries = mutableListOf<Entry>()
        val uvIndexEntries = mutableListOf<BarEntry>()
        val times = mutableListOf<String>()
        var temperatureFound: Boolean
        var feelsLikeFound: Boolean
        var precipitationFound: Boolean
        var windSpeedFound: Boolean
        var windGustFound: Boolean
        var windDirectionFound: Boolean
        var pressureFound: Boolean
        var humidityFound: Boolean

        LocalDateTimeRange(
            date.atStartOfDay(),
            date.plusDays(1).atStartOfDay().minusHours(1)
        ).forEachIndexed { i, localDateTime ->
            temperatureFound = false
            feelsLikeFound = false
            precipitationFound = false
            windSpeedFound = false
            windGustFound = false
            windDirectionFound = false
            pressureFound = false
            humidityFound = false
            val counter = i.toFloat()

            // Set showMinutes12Format as false
            // on hourly data they don't matter and they cause UI issues
            times.add(localDateTime.getFormattedTime(context, false))

            hourlyWeatherData.forEach { hourlyWeather ->
                if (hourlyWeather.timestamp.toLocalDateTime() == localDateTime) {
                    hourlyWeather.temperature?.let {
                        temperatureEntries.add(Entry(counter, Weather.convertTemp(it, 1) as Float))
                        temperatureFound = true
                    }
                    hourlyWeather.feelsLike?.let {
                        feelsLikeEntries.add(Entry(counter, Weather.convertTemp(it, 1) as Float))
                        feelsLikeFound = true
                    }
                    hourlyWeather.precipitation?.let {
                        precipEntries.add(Entry(counter, Weather.convertPrecipitation(it) as Float))
                        precipitationFound = true
                    }

                    // Get the wind speed and direction formatted
                    val windSpeedValue =
                        Weather.convertWindSpeed(hourlyWeather.windSpeed)?.toFloat()
                    val windGustValue = Weather.convertWindSpeed(hourlyWeather.windGust)?.toFloat()
                    var windDirection: Drawable? = null
                    hourlyWeather.windDirection?.let {
                        val index = UnitConverter.getIndexOfCardinal(it)
                        windDirection = resHelper.getWindDirectionDrawable(index)
                        windDirectionEntries.add(Entry(counter, it.toFloat()))
                        windDirectionFound = true
                    }

                    /**
                     * Show wind direction only when
                     * the icon is available &&
                     * wind speed || wind gust are not null and greater than zero
                     */
                    val shouldShowDirection = windDirection != null &&
                        (((windSpeedValue ?: 0.0F) > 0) || ((windGustValue ?: 0.0F) > 0))

                    windSpeedValue?.let {
                        if (shouldShowDirection) {
                            windSpeedEntries.add(Entry(counter, it, windDirection))
                        } else {
                            windSpeedEntries.add(Entry(counter, it))
                        }
                        windSpeedFound = true
                    }

                    windGustValue?.let {
                        windGustEntries.add(Entry(counter, it))
                        windGustFound = true
                    }

                    hourlyWeather.pressure?.let {
                        pressureEntries.add(Entry(counter, Weather.convertPressure(it) as Float))
                        pressureFound = true
                    }
                    hourlyWeather.humidity?.let {
                        humidityEntries.add(Entry(counter, it.toFloat()))
                        humidityFound = true
                    }

                    hourlyWeather.uvIndex?.let {
                        uvIndexEntries.add(BarEntry(counter, it.toFloat()))
                    }
                }
            }

            if (!temperatureFound) temperatureEntries.add(Entry(counter, Float.NaN))
            if (!feelsLikeFound) feelsLikeEntries.add(Entry(counter, Float.NaN))
            if (!precipitationFound) precipEntries.add(Entry(counter, Float.NaN))
            if (!windSpeedFound) windSpeedEntries.add(Entry(counter, Float.NaN))
            if (!windGustFound) windGustEntries.add(Entry(counter, Float.NaN))
            if (!windDirectionFound) windDirectionEntries.add(Entry(counter, Float.NaN))
            if (!pressureFound) pressureEntries.add(Entry(counter, Float.NaN))
            if (!humidityFound) humidityEntries.add(Entry(counter, Float.NaN))
        }

        return HistoryCharts(
            date = date,
            temperature = LineChartData(
                resHelper.getString(R.string.temperature),
                Weather.getPreferredUnit(
                    resHelper.getString(R.string.key_temperature_preference),
                    resHelper.getString(R.string.temperature_celsius)
                ),
                times,
                temperatureEntries
            ),
            feelsLike = LineChartData(
                resHelper.getString(R.string.feels_like),
                Weather.getPreferredUnit(
                    resHelper.getString(R.string.key_temperature_preference),
                    resHelper.getString(R.string.temperature_celsius)
                ),
                times,
                feelsLikeEntries
            ),
            precipitation = LineChartData(
                resHelper.getString(R.string.precipitation),
                Weather.getPrecipitationPreferredUnit(false),
                times,
                precipEntries
            ),
            windSpeed = LineChartData(
                resHelper.getString(R.string.wind_speed),
                Weather.getPreferredUnit(
                    resHelper.getString(R.string.key_wind_speed_preference),
                    resHelper.getString(R.string.wind_speed_ms)
                ),
                times,
                windSpeedEntries
            ),
            windGust = LineChartData(
                resHelper.getString(R.string.wind_gust),
                Weather.getPreferredUnit(
                    resHelper.getString(R.string.key_wind_speed_preference),
                    resHelper.getString(R.string.wind_speed_ms)
                ),
                times,
                windGustEntries
            ),
            windDirection = LineChartData(
                resHelper.getString(R.string.wind_direction),
                Weather.getPreferredUnit(
                    resHelper.getString(R.string.key_wind_direction_preference),
                    resHelper.getString(R.string.wind_direction_cardinal)
                ),
                times,
                windDirectionEntries
            ),
            humidity = LineChartData(
                resHelper.getString(R.string.humidity),
                resHelper.getString(R.string.percent),
                times,
                humidityEntries
            ),
            pressure = LineChartData(
                resHelper.getString(R.string.pressure),
                Weather.getPreferredUnit(
                    resHelper.getString(R.string.key_pressure_preference),
                    resHelper.getString(R.string.pressure_hpa)
                ),
                times,
                pressureEntries
            ),
            uvIndex = BarChartData(
                resHelper.getString(R.string.uv_index),
                resHelper.getString(R.string.uv_index_unit),
                times,
                uvIndexEntries
            )
        )
    }
}
