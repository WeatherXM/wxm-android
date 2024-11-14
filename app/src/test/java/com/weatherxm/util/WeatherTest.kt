package com.weatherxm.util

import com.weatherxm.R
import com.weatherxm.TestConfig.context
import com.weatherxm.ui.common.WeatherUnit
import com.weatherxm.ui.common.WeatherUnitType
import com.weatherxm.util.Weather.getFormattedHumidity
import com.weatherxm.util.Weather.getFormattedPrecipitation
import com.weatherxm.util.Weather.getFormattedPrecipitationProbability
import com.weatherxm.util.Weather.getFormattedPressure
import com.weatherxm.util.Weather.getFormattedSolarRadiation
import com.weatherxm.util.Weather.getFormattedTemperature
import com.weatherxm.util.Weather.getFormattedUV
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import java.text.NumberFormat
import java.util.Locale

class WeatherTest : KoinTest, BehaviorSpec({

    beforeSpec {
        startKoin {
            modules(
                module {
                    single<NumberFormat> {
                        NumberFormat.getInstance(Locale.US)
                    }
                }
            )
        }
        mockkObject(UnitSelector)

        every { UnitSelector.getTemperatureUnit(context) } returns WeatherUnit(
            WeatherUnitType.CELSIUS,
            context.getString(R.string.temperature_celsius)
        )
        every { UnitSelector.getPrecipitationUnit(context, false) } returns WeatherUnit(
            WeatherUnitType.MILLIMETERS,
            context.getString(R.string.precipitation_mm)
        )
        every { UnitSelector.getPrecipitationUnit(context, true) } returns WeatherUnit(
            WeatherUnitType.MILLIMETERS,
            context.getString(R.string.precipitation_mm_hour)
        )
        every { UnitSelector.getWindUnit(context) } returns WeatherUnit(
            WeatherUnitType.MS,
            context.getString(R.string.wind_speed_ms)
        )
        every { UnitSelector.getWindDirectionUnit(context) } returns WeatherUnit(
            WeatherUnitType.CARDINAL,
            context.getString(R.string.wind_direction_cardinal)
        )
        every { UnitSelector.getPressureUnit(context) } returns WeatherUnit(
            WeatherUnitType.HPA,
            context.getString(R.string.pressure_hpa)
        )
    }

    given("A Weather icon") {
        testWeatherIcon("not-available", R.raw.anim_not_available, null)
        testWeatherIcon("clear-day", R.raw.anim_weather_clear_day, R.drawable.ic_weather_clear_day)
        testWeatherIcon(
            "clear-night", R.raw.anim_weather_clear_night, R.drawable.ic_weather_clear_night
        )
        testWeatherIcon(
            "partly-cloudy-day",
            R.raw.anim_weather_partly_cloudy_day,
            R.drawable.ic_weather_partly_cloudy_day
        )
        testWeatherIcon(
            "partly-cloudy-night",
            R.raw.anim_weather_partly_cloudy_night,
            R.drawable.ic_weather_partly_cloudy_night
        )
        testWeatherIcon(
            "overcast-day", R.raw.anim_weather_overcast_day, R.drawable.ic_weather_overcast_day
        )
        testWeatherIcon(
            "overcast-night",
            R.raw.anim_weather_overcast_night,
            R.drawable.ic_weather_overcast_night
        )
        testWeatherIcon("drizzle", R.raw.anim_weather_drizzle, R.drawable.ic_weather_drizzle)
        testWeatherIcon("rain", R.raw.anim_weather_rain, R.drawable.ic_weather_rain)
        testWeatherIcon(
            "thunderstorms-rain",
            R.raw.anim_weather_thunderstorms_rain,
            R.drawable.ic_weather_thunderstorms_rain
        )
        testWeatherIcon("snow", R.raw.anim_weather_snow, R.drawable.ic_weather_snow)
        testWeatherIcon("sleet", R.raw.anim_weather_sleet, R.drawable.ic_weather_sleet)
        testWeatherIcon("wind", R.raw.anim_weather_wind, R.drawable.ic_weather_windy)
        testWeatherIcon("fog", R.raw.anim_weather_fog, R.drawable.ic_weather_fog)
        testWeatherIcon("cloudy", R.raw.anim_weather_cloudy, R.drawable.ic_weather_cloudy)
        testWeatherIcon(null, R.raw.anim_not_available, null)
    }

    context("Format UV") {
        given("A UV value") {
            When("value is null") {
                testNullValue("", ::getFormattedUV)
            }
            When("value is not null") {
                and("value is less than 2") {
                    testUV(0, R.string.uv_low)
                    testUV(1, R.string.uv_low)
                }
                and("value is between 2 and 5") {
                    testUV(3, R.string.uv_moderate)
                    testUV(5, R.string.uv_moderate)
                }
                and("value is between 5 and 7") {
                    testUV(6, R.string.uv_high)
                    testUV(7, R.string.uv_high)
                }
                and("value is between 7 and 10") {
                    testUV(8, R.string.uv_very_high)
                    testUV(9, R.string.uv_very_high)
                }
                and("value is greater than 10") {
                    testUV(11, R.string.uv_extreme)
                }
                and("unit (classification) should NOT be included") {
                    then("it should return the value without the unit") {
                        getFormattedUV(context, 5, false) shouldBe "5"
                    }
                }
            }
        }
    }

    context("Format Humidity") {
        testPercentageMeasurements("Humidity", 50, ::getFormattedHumidity)
    }

    context("Format Precipitation Probability") {
        testPercentageMeasurements(
            "Precipitation Probability", 50, ::getFormattedPrecipitationProbability
        )
    }

    context("Format Temperature") {
        given("A temperature value") {
            When("value is null") {
                testNullFloatValue("°C", ::getFormattedTemperature)
            }
            When("value is not null") {
                and("is in Celsius") {
                    testTemperature("°C", 25, 25.5F)
                }
                and("is in Fahrenheit") {
                    every { UnitSelector.getTemperatureUnit(context) } returns WeatherUnit(
                        WeatherUnitType.FAHRENHEIT,
                        context.getString(R.string.temperature_fahrenheit)
                    )
                    testTemperature("°F", 77, 77.9F)
                }
            }
        }
    }

    context("Format Solar Radiation") {
        given("A Solar Radiation value") {
            When("value is null") {
                testNullFloatValue("W/m2", ::getFormattedSolarRadiation)
            }
            When("value is not null") {
                testSolarRadiation(25.4F)
            }
        }
    }

    context("Format Pressure") {
        given("A Pressure value") {
            When("value is null") {
                testNullFloatValue("hPa", ::getFormattedPressure)
            }
            When("value is not null") {
                and("is in hPa") {
                    testPressure(WeatherUnit(WeatherUnitType.HPA, "hPa"), "1,000.4")
                }
                and("unit is in inHg") {
                    every { UnitSelector.getPressureUnit(context) } returns WeatherUnit(
                        WeatherUnitType.INHG,
                        context.getString(R.string.pressure_inHg)
                    )
                    testPressure(WeatherUnit(WeatherUnitType.INHG, "inHg"), "29.54")
                }
            }
        }
    }

    context("Format Precipitation") {
        given("A Precipitation value") {
            When("value is null") {
                testNullFloatValue("mm/h", ::getFormattedPrecipitation)
            }
            When("value is not null") {
                and("is in millimeters") {
                    testPrecipitation(WeatherUnitType.MILLIMETERS, "mm/h", "mm", 10.6F)
                }
                and("unit is in inches") {
                    every { UnitSelector.getPrecipitationUnit(context, false) } returns WeatherUnit(
                        WeatherUnitType.INCHES,
                        context.getString(R.string.precipitation_in)
                    )
                    every { UnitSelector.getPrecipitationUnit(context, true) } returns WeatherUnit(
                        WeatherUnitType.INCHES,
                        context.getString(R.string.precipitation_in_hour)
                    )
                    testPrecipitation(WeatherUnitType.INCHES, "in/h", "in", 0.42F)
                }
            }
        }
    }

    context("Format Wind Speed") {
        given("A Wind Speed value") {
            When("value is null") {
                testNullWindValue()
            }
            When("value is not null") {
                and("is in m/s") {
                    testWind("m/s", 10.6F, false)
                }
                and("is in beaufort") {
                    every { UnitSelector.getWindUnit(context) } returns WeatherUnit(
                        WeatherUnitType.BEAUFORT,
                        context.getString(R.string.wind_speed_beaufort)
                    )
                    testWind("bf", 5F, true)
                }
                and("is in km/h") {
                    every { UnitSelector.getWindUnit(context) } returns WeatherUnit(
                        WeatherUnitType.KMH,
                        context.getString(R.string.wind_speed_kmh)
                    )
                    testWind("km/h", 38F, false)
                }
                and("is in mph") {
                    every { UnitSelector.getWindUnit(context) } returns WeatherUnit(
                        WeatherUnitType.MPH,
                        context.getString(R.string.wind_speed_mph)
                    )
                    testWind("mph", 23.6F, false)
                }
                and("is in knots") {
                    every { UnitSelector.getWindUnit(context) } returns WeatherUnit(
                        WeatherUnitType.KNOTS,
                        context.getString(R.string.wind_speed_knots)
                    )
                    testWind("knots", 20.5F, false)
                }
            }
        }
    }

    afterSpec {
        stopKoin()
        unmockkObject(UnitSelector)
    }
})
