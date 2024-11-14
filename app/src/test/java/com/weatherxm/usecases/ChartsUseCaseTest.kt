package com.weatherxm.usecases

import android.text.format.DateFormat
import androidx.appcompat.content.res.AppCompatResources
import com.github.mikephil.charting.data.Entry
import com.weatherxm.R
import com.weatherxm.TestConfig.context
import com.weatherxm.TestConfig.resources
import com.weatherxm.TestConfig.sharedPref
import com.weatherxm.TestUtils.isEqual
import com.weatherxm.data.HOUR_FORMAT_24H
import com.weatherxm.data.models.HourlyWeather
import com.weatherxm.data.services.CacheService
import com.weatherxm.ui.common.Charts
import com.weatherxm.ui.common.LineChartData
import com.weatherxm.util.LocalDateTimeRange
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class ChartsUseCaseTest : BehaviorSpec({
    val usecase = ChartsUseCaseImpl(context)

    val zonedDateTime = ZonedDateTime.now()
    val localDate = zonedDateTime.toLocalDate()
    val hourlyWeatherData = mutableListOf<HourlyWeather>()


    val timestamps = mutableListOf(
        "00:00", "01:00", "02:00", "03:00", "04:00", "05:00", "06:00", "07:00", "08:00", "09:00",
        "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00", "18:00", "19:00",
        "20:00", "21:00", "22:00", "23:00"
    )
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

    val chartsData = Charts(
        date = localDate,
        temperature = LineChartData(timestamps, temperatureEntries),
        feelsLike = LineChartData(timestamps, feelsLikeEntries),
        precipitation = LineChartData(timestamps, precipEntries),
        precipitationAccumulated = LineChartData(timestamps, precipAccumulatedEntries),
        precipProbability = LineChartData(timestamps, precipProbabilityEntries),
        windSpeed = LineChartData(timestamps, windSpeedEntries),
        windGust = LineChartData(timestamps, windGustEntries),
        windDirection = LineChartData(timestamps, windDirectionEntries),
        humidity = LineChartData(timestamps, humidityEntries),
        pressure = LineChartData(timestamps, pressureEntries),
        uv = LineChartData(timestamps, uvEntries),
        solarRadiation = LineChartData(timestamps, solarRadiationEntries)
    )


    beforeSpec {
        startKoin {
            modules(module {
                single<CacheService> { mockk() }
                single<DateTimeFormatter>(named(HOUR_FORMAT_24H)) {
                    DateTimeFormatter.ofPattern(HOUR_FORMAT_24H)
                }
                single { resources }
                single { sharedPref }
            })
        }

        LocalDateTimeRange(
            localDate.atStartOfDay(),
            localDate.plusDays(1).atStartOfDay().minusHours(2)
        ).forEachIndexed { _, localDateTime ->
            hourlyWeatherData.add(
                HourlyWeather(
                    localDateTime.atZone(ZoneId.systemDefault()),
                    10F,
                    10F,
                    35F,
                    34F,
                    null,
                    75,
                    5F,
                    7F,
                    null,
                    14,
                    7,
                    null,
                    1010F,
                    null,
                    500F
                )
            )
        }

        /**
         * Add a gap at the end
         */
        LocalDateTimeRange(
            localDate.plusDays(1).atStartOfDay().minusHours(2),
            localDate.plusDays(1).atStartOfDay().minusHours(1)
        ).forEachIndexed { _, localDateTime ->
            hourlyWeatherData.add(
                HourlyWeather(
                    localDateTime.atZone(ZoneId.systemDefault()),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
                )
            )

            mockkStatic(AppCompatResources::class)
            every {
                AppCompatResources.getDrawable(context, R.drawable.ic_weather_wind)
            } returns null
        }

        repeat(23) { i ->
            temperatureEntries.add(Entry(i.toFloat(), 35F))
            feelsLikeEntries.add(Entry(i.toFloat(), 34F))
            precipEntries.add(Entry(i.toFloat(), 10F))
            precipAccumulatedEntries.add(Entry(i.toFloat(), 10F))
            precipProbabilityEntries.add(Entry(i.toFloat(), 14F))
            windSpeedEntries.add(Entry(i.toFloat(), 5F))
            windGustEntries.add(Entry(i.toFloat(), 7F))
            pressureEntries.add(Entry(i.toFloat(), 1010F))
            windDirectionEntries.add(Entry(i.toFloat(), Float.NaN))
            uvEntries.add(Entry(i.toFloat(), 7F))
            humidityEntries.add(Entry(i.toFloat(), 75F))
            solarRadiationEntries.add(Entry(i.toFloat(), 500F))
        }
        /**
         * Add a gap at the end
         */
        temperatureEntries.add(Entry(23F, Float.NaN))
        feelsLikeEntries.add(Entry(23F, Float.NaN))
        precipEntries.add(Entry(23F, Float.NaN))
        precipAccumulatedEntries.add(Entry(23F, Float.NaN))
        precipProbabilityEntries.add(Entry(23F, Float.NaN))
        windSpeedEntries.add(Entry(23F, Float.NaN))
        windGustEntries.add(Entry(23F, Float.NaN))
        pressureEntries.add(Entry(23F, Float.NaN))
        windDirectionEntries.add(Entry(23F, Float.NaN))
        uvEntries.add(Entry(23F, Float.NaN))
        humidityEntries.add(Entry(23F, Float.NaN))
        solarRadiationEntries.add(Entry(23F, Float.NaN))

        every { DateFormat.is24HourFormat(context) } returns true
    }

    context("Transform a date and a list of HourlyWeather data to the Charts UI Model") {
        given("A date and a list of HourlyWeather data") {
            then("it should return the Charts UI Model") {
                usecase.createHourlyCharts(localDate, hourlyWeatherData).isEqual(chartsData)
            }
        }
    }

    afterSpec {
        stopKoin()
    }
})
