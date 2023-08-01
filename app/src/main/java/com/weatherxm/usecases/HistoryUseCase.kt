package com.weatherxm.usecases

import android.content.Context
import android.graphics.drawable.Drawable
import arrow.core.Either
import com.github.mikephil.charting.data.Entry
import com.weatherxm.data.Failure
import com.weatherxm.data.HourlyWeather
import com.weatherxm.data.repository.WeatherHistoryRepository
import com.weatherxm.ui.common.UIDevice
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
        device: UIDevice,
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
        device: UIDevice,
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
        val precipAccumulatedEntries = mutableListOf<Entry>()
        val windSpeedEntries = mutableListOf<Entry>()
        val windGustEntries = mutableListOf<Entry>()
        val windDirectionEntries = mutableListOf<Entry>()
        val humidityEntries = mutableListOf<Entry>()
        val pressureEntries = mutableListOf<Entry>()
        val uvEntries = mutableListOf<Entry>()
        val solarRadiationEntries = mutableListOf<Entry>()
        val times = mutableListOf<String>()

        LocalDateTimeRange(
            date.atStartOfDay(),
            date.plusDays(1).atStartOfDay().minusHours(1)
        ).forEachIndexed { i, localDateTime ->
            val counter = i.toFloat()
            val emptyEntry = Entry(counter, Float.NaN)

            // Set showMinutes12Format as false
            // on hourly data they don't matter and they cause UI issues
            times.add(localDateTime.getFormattedTime(context, false))

            hourlyWeatherData.firstOrNull {
                it.timestamp.toLocalDateTime() == localDateTime
            }?.let { hourlyWeather ->
                hourlyWeather.temperature?.let {
                    temperatureEntries.add(Entry(counter, Weather.convertTemp(it, 1) as Float))
                } ?: temperatureEntries.add(emptyEntry)

                hourlyWeather.feelsLike?.let {
                    feelsLikeEntries.add(Entry(counter, Weather.convertTemp(it, 1) as Float))
                } ?: feelsLikeEntries.add(emptyEntry)

                hourlyWeather.precipitation?.let {
                    precipEntries.add(Entry(counter, Weather.convertPrecipitation(it) as Float))
                } ?: precipEntries.add(emptyEntry)

                hourlyWeather.precipAccumulated?.let {
                    precipAccumulatedEntries.add(
                        Entry(counter, Weather.convertPrecipitation(it) as Float)
                    )
                } ?: windSpeedEntries.add(emptyEntry)

                // Get the wind speed and direction formatted
                val windSpeedValue = Weather.convertWindSpeed(hourlyWeather.windSpeed)?.toFloat()
                val windGustValue = Weather.convertWindSpeed(hourlyWeather.windGust)?.toFloat()
                var windDirection: Drawable? = null
                hourlyWeather.windDirection?.let {
                    val index = UnitConverter.getIndexOfCardinal(it)
                    windDirection = resHelper.getWindDirectionDrawable(index)
                    windDirectionEntries.add(Entry(counter, it.toFloat()))
                } ?: windDirectionEntries.add(emptyEntry)

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
                } ?: windSpeedEntries.add(emptyEntry)

                windGustValue?.let {
                    windGustEntries.add(Entry(counter, it))
                } ?: windGustEntries.add(emptyEntry)

                hourlyWeather.pressure?.let {
                    pressureEntries.add(Entry(counter, Weather.convertPressure(it) as Float))
                } ?: pressureEntries.add(emptyEntry)

                hourlyWeather.humidity?.let {
                    humidityEntries.add(Entry(counter, it.toFloat()))
                } ?: humidityEntries.add(emptyEntry)

                hourlyWeather.uvIndex?.let {
                    uvEntries.add(Entry(counter, it.toFloat()))
                } ?: uvEntries.add(emptyEntry)

                hourlyWeather.solarIrradiance?.let {
                    solarRadiationEntries.add(Entry(counter, Weather.roundToDecimals(it)))
                } ?: solarRadiationEntries.add(Entry(counter, Float.NaN))

            } ?: kotlin.run {
                temperatureEntries.add(emptyEntry)
                feelsLikeEntries.add(emptyEntry)
                precipEntries.add(emptyEntry)
                precipAccumulatedEntries.add(emptyEntry)
                windSpeedEntries.add(emptyEntry)
                windGustEntries.add(emptyEntry)
                windDirectionEntries.add(emptyEntry)
                pressureEntries.add(emptyEntry)
                humidityEntries.add(emptyEntry)
                uvEntries.add(emptyEntry)
                solarRadiationEntries.add(emptyEntry)
            }
        }

        return HistoryCharts(
            date = date,
            temperature = LineChartData(times, temperatureEntries),
            feelsLike = LineChartData(times, feelsLikeEntries),
            precipitation = LineChartData(times, precipEntries),
            precipitationAccumulated = LineChartData(times, precipAccumulatedEntries),
            windSpeed = LineChartData(times, windSpeedEntries),
            windGust = LineChartData(times, windGustEntries),
            windDirection = LineChartData(times, windDirectionEntries),
            humidity = LineChartData(times, humidityEntries),
            pressure = LineChartData(times, pressureEntries),
            uv = LineChartData(times, uvEntries),
            solarRadiation = LineChartData(times, solarRadiationEntries)
        )
    }
}
