package com.weatherxm.usecases

import android.content.Context
import android.graphics.drawable.Drawable
import com.github.mikephil.charting.data.Entry
import com.weatherxm.data.models.HourlyWeather
import com.weatherxm.ui.common.Charts
import com.weatherxm.ui.common.LineChartData
import com.weatherxm.util.DateTimeHelper.getFormattedTime
import com.weatherxm.util.LocalDateTimeRange
import com.weatherxm.util.NumberUtils
import com.weatherxm.util.Weather
import com.weatherxm.util.Weather.convertPrecipitation
import com.weatherxm.util.Weather.convertWindSpeed
import java.time.Duration
import java.time.LocalDate

class ChartsUseCaseImpl(private val context: Context) : ChartsUseCase {
    companion object {
        const val FORECAST_CHART_STEP_DEFAULT = 3L
        const val FORECAST_CHART_STEP_PREMIUM = 1L
    }

    /**
     * Suppress long and Complex method warning by detekt because it is just a bunch of `.let`
     * and `.if` statements
     */
    @Suppress("LongMethod", "ComplexMethod")
    override fun createHourlyCharts(
        date: LocalDate,
        hourlyWeatherData: List<HourlyWeather>,
        chartStep: Duration
    ): Charts {
        val temperatureEntries = mutableListOf<Entry>()
        val feelsLikeEntries = mutableListOf<Entry>()
        val precipEntries = mutableListOf<Entry>()
        val precipAccumulatedEntries = mutableListOf<Entry>()
        val precipProbabilityEntries = mutableListOf<Entry>()
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
            date.plusDays(1).atStartOfDay().minusHours(1),
            chartStep
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
                    temperatureEntries.add(
                        Entry(counter, Weather.convertTemp(context, it, 1) as Float)
                    )
                } ?: temperatureEntries.add(emptyEntry)

                hourlyWeather.feelsLike?.let {
                    feelsLikeEntries.add(
                        Entry(counter, Weather.convertTemp(context, it, 1) as Float)
                    )
                } ?: feelsLikeEntries.add(emptyEntry)

                hourlyWeather.precipitation?.let {
                    precipEntries.add(Entry(counter, convertPrecipitation(context, it) as Float))
                } ?: precipEntries.add(emptyEntry)

                hourlyWeather.precipAccumulated?.let {
                    precipAccumulatedEntries.add(
                        Entry(counter, convertPrecipitation(context, it) as Float)
                    )
                } ?: precipAccumulatedEntries.add(emptyEntry)

                hourlyWeather.precipProbability?.let {
                    precipProbabilityEntries.add(Entry(counter, it.toFloat()))
                } ?: precipProbabilityEntries.add(emptyEntry)

                // Get the wind speed and direction formatted
                val windSpeedValue = hourlyWeather.windSpeed?.let {
                    convertWindSpeed(context, it)
                }?.toFloat()
                val windGustValue = hourlyWeather.windGust?.let {
                    convertWindSpeed(context, it)
                }?.toFloat()
                val windDirection: Drawable? =
                    Weather.getWindDirectionDrawable(context, hourlyWeather.windDirection)
                hourlyWeather.windDirection?.let {
                    windDirectionEntries.add(Entry(counter, it.toFloat()))
                } ?: windDirectionEntries.add(emptyEntry)

                /**
                 * Show wind direction only when
                 * the icon is available &&
                 * wind speed || wind gust are not null and greater than zero
                 */
                val shouldShowDirection = windDirection != null &&
                    ((windSpeedValue ?: 0.0F) > 0 || (windGustValue ?: 0.0F) > 0)

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
                    pressureEntries.add(
                        Entry(counter, Weather.convertPressure(context, it) as Float)
                    )
                } ?: pressureEntries.add(emptyEntry)

                hourlyWeather.humidity?.let {
                    humidityEntries.add(Entry(counter, it.toFloat()))
                } ?: humidityEntries.add(emptyEntry)

                hourlyWeather.uvIndex?.let {
                    uvEntries.add(Entry(counter, it.toFloat()))
                } ?: uvEntries.add(emptyEntry)

                hourlyWeather.solarIrradiance?.let {
                    solarRadiationEntries.add(Entry(counter, NumberUtils.roundToDecimals(it)))
                } ?: solarRadiationEntries.add(emptyEntry)

            } ?: kotlin.run {
                temperatureEntries.add(emptyEntry)
                feelsLikeEntries.add(emptyEntry)
                precipEntries.add(emptyEntry)
                precipAccumulatedEntries.add(emptyEntry)
                precipProbabilityEntries.add(emptyEntry)
                windSpeedEntries.add(emptyEntry)
                windGustEntries.add(emptyEntry)
                windDirectionEntries.add(emptyEntry)
                pressureEntries.add(emptyEntry)
                humidityEntries.add(emptyEntry)
                uvEntries.add(emptyEntry)
                solarRadiationEntries.add(emptyEntry)
            }
        }

        return Charts(
            date = date,
            temperature = LineChartData(times, temperatureEntries),
            feelsLike = LineChartData(times, feelsLikeEntries),
            precipitation = LineChartData(times, precipEntries),
            precipitationAccumulated = LineChartData(times, precipAccumulatedEntries),
            precipProbability = LineChartData(times, precipProbabilityEntries),
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
