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
import com.weatherxm.ui.BarChartData
import com.weatherxm.ui.HistoryCharts
import com.weatherxm.ui.LineChartData
import com.weatherxm.util.DateTimeHelper.getHourMinutesFromISO
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

    private fun createTemperatureLineChartData(
        time: MutableList<String>,
        entries: MutableList<Entry>
    ): LineChartData {
        return LineChartData(
            resHelper.getString(R.string.temperature),
            Weather.getPreferredUnit(
                resHelper.getString(R.string.key_temperature_preference),
                resHelper.getString(R.string.temperature_celsius)
            ),
            timestamps = time,
            entries = entries
        )
    }

    private fun createFeelsLikeLineChartData(
        time: MutableList<String>,
        entries: MutableList<Entry>
    ): LineChartData {
        return LineChartData(
            resHelper.getString(R.string.feels_like),
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
            resHelper.getString(R.string.uv_index_unit),
            timestamps = time,
            entries = entries
        )
    }

    /*
    * Suppress complex method warning by detekt because it is just a bunch of `.let` statements
     */
    @Suppress("ComplexMethod")
    private fun createHourlyCharts(
        date: LocalDate,
        hourlyWeatherData: List<HourlyWeather>
    ): HistoryCharts {
        var counter = 0F

        val temperatureEntries = mutableListOf<Entry>()
        val feelsLikeEntries = mutableListOf<Entry>()
        val precipEntries = mutableListOf<Entry>()
        val windSpeedEntries = mutableListOf<Entry>()
        val windGustEntries = mutableListOf<Entry>()
        val windDirectionEntries = mutableListOf<Entry>()
        val humidityEntries = mutableListOf<Entry>()
        val pressureEntries = mutableListOf<Entry>()
        val uvIndexEntries = mutableListOf<BarEntry>()
        val time = mutableListOf<String>()

        hourlyWeatherData.forEach { hourlyWeather ->
            // Set showMinutes12Format as false
            // on hourly data they don't matter and they cause UI issues
            time.add(getHourMinutesFromISO(context, hourlyWeather.timestamp.toString(), false))

            hourlyWeather.temperature?.let {
                temperatureEntries.add(Entry(counter, Weather.convertTemp(it, 1) as Float))
            }

            hourlyWeather.feelsLike?.let {
                feelsLikeEntries.add(Entry(counter, Weather.convertTemp(it, 1) as Float))
            }

            hourlyWeather.precipitation?.let {
                precipEntries.add(Entry(counter, Weather.convertPrecipitation(it) as Float))
            }

            // Get the wind speed and direction formatted
            val windSpeedValue = Weather.convertWindSpeed(hourlyWeather.windSpeed)?.toFloat()
            val windGustValue = Weather.convertWindSpeed(hourlyWeather.windGust)?.toFloat()
            var windDirection: Drawable? = null
            hourlyWeather.windDirection?.let {
                val index = UnitConverter.getIndexOfCardinal(it)
                windDirection = resHelper.getWindDirectionDrawable(index)
                windDirectionEntries.add(Entry(counter, it.toFloat()))
            }

            /*
            * Show wind direction only when the icon is available and wind speed OR wind gust
            * are not null and great than zero
             */
            val shouldShowDirection = windDirection != null &&
                (((windSpeedValue ?: 0.0F) > 0) || ((windGustValue ?: 0.0F) > 0))

            windSpeedValue?.let {
                if (shouldShowDirection) {
                    windSpeedEntries.add(Entry(counter, it, windDirection))
                } else {
                    windSpeedEntries.add(Entry(counter, it))
                }
            }

            windGustValue?.let {
                windGustEntries.add(Entry(counter, it))
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

            counter++
        }

        return HistoryCharts(
            date = date,
            temperature = createTemperatureLineChartData(time, temperatureEntries),
            feelsLike = createFeelsLikeLineChartData(time, feelsLikeEntries),
            precipitation = createPrecipitationLineChartData(time, precipEntries),
            windSpeed = createWindSpeedLineChartData(time, windSpeedEntries),
            windGust = createWindGustLineChartData(time, windGustEntries),
            windDirection = createWindDirectionLineChartData(time, windDirectionEntries),
            humidity = createHumidityLineChartData(time, humidityEntries),
            pressure = createPressureLineChartData(time, pressureEntries),
            uvIndex = createUvIndexBarChartData(time, uvIndexEntries)
        )
    }
}
