package com.weatherxm.util

import android.content.SharedPreferences
import com.weatherxm.R
import com.weatherxm.data.services.CacheService.Companion.KEY_PRECIP
import com.weatherxm.data.services.CacheService.Companion.KEY_PRESSURE
import com.weatherxm.data.services.CacheService.Companion.KEY_TEMPERATURE
import com.weatherxm.data.services.CacheService.Companion.KEY_WIND
import com.weatherxm.data.services.CacheService.Companion.KEY_WIND_DIR
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
import io.mockk.mockk
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class WeatherTest : BehaviorSpec({
    val resources = mockk<Resources>()
    val sharedPreferences = mockk<SharedPreferences>()

    beforeSpec {
        startKoin {
            modules(
                module {
                    single { resources }
                    single { sharedPreferences }
                }
            )
        }

        every { resources.getString(R.string.uv_low) } returns "Low"
        every { resources.getString(R.string.uv_moderate) } returns "Moderate"
        every { resources.getString(R.string.uv_high) } returns "High"
        every { resources.getString(R.string.uv_very_high) } returns "Very High"
        every { resources.getString(R.string.uv_extreme) } returns "Extreme"
        every { resources.getString(KEY_TEMPERATURE) } returns "temperature_unit"
        every { resources.getString(R.string.temperature_celsius) } returns "°C"
        every { resources.getString(R.string.degrees_mark) } returns "°"
        every { sharedPreferences.getString("temperature_unit", "°C") } returns "°C"
        every { resources.getString(R.string.solar_radiation_unit) } returns "W/m2"
        every { resources.getString(KEY_PRESSURE) } returns "pressure_unit"
        every { resources.getString(R.string.pressure_hpa) } returns "hPa"
        every { sharedPreferences.getString("pressure_unit", "hPa") } returns "hPa"
        every { resources.getString(KEY_PRECIP) } returns "precip_unit"
        every { resources.getString(R.string.precipitation_mm) } returns "mm"
        every { resources.getString(R.string.precipitation_mm_hour) } returns "mm/h"
        every { resources.getString(R.string.precipitation_in) } returns "in"
        every { resources.getString(R.string.precipitation_in_hour) } returns "in/h"
        every { sharedPreferences.getString("precip_unit", "mm") } returns "mm"
        every { resources.getString(KEY_WIND) } returns "wind"
        every { resources.getString(R.string.wind_speed_ms) } returns "m/s"
        every { resources.getString(R.string.wind_speed_beaufort) } returns "bf"
        every { resources.getString(R.string.wind_speed_kmh) } returns "km/h"
        every { resources.getString(R.string.wind_speed_mph) } returns "mph"
        every { resources.getString(R.string.wind_speed_knots) } returns "knots"
        every { sharedPreferences.getString("wind", "m/s") } returns "m/s"
        every { resources.getString(KEY_WIND_DIR) } returns "wind_direction"
        every { resources.getString(R.string.wind_direction_cardinal) } returns "Cardinal"
        every { sharedPreferences.getString("wind_direction", "Cardinal") } returns "Cardinal"
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
                    testUV(resources, 0, R.string.uv_low)
                    testUV(resources, 1, R.string.uv_low)
                }
                and("value is between 2 and 5") {
                    testUV(resources, 3, R.string.uv_moderate)
                    testUV(resources, 5, R.string.uv_moderate)
                }
                and("value is between 5 and 7") {
                    testUV(resources, 6, R.string.uv_high)
                    testUV(resources, 7, R.string.uv_high)
                }
                and("value is between 7 and 10") {
                    testUV(resources, 8, R.string.uv_very_high)
                    testUV(resources, 9, R.string.uv_very_high)
                }
                and("value is greater than 10") {
                    testUV(resources, 11, R.string.uv_extreme)
                }
                and("unit (classification) should NOT be included") {
                    then("it should return the value without the unit") {
                        getFormattedUV(5, false) shouldBe "5"
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
                    every { sharedPreferences.getString("temperature_unit", "°C") } returns "°F"
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
                    testPressure("hPa", 1000.4F)
                }
                and("unit is in inHg") {
                    every { sharedPreferences.getString("pressure_unit", "hPa") } returns "inHg"
                    testPressure("inHg", 29.54F)
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
                    testPrecipitation("mm/h", "mm", 10.6F)
                }
                and("unit is in inches") {
                    every { sharedPreferences.getString("precip_unit", "mm") } returns "in"
                    testPrecipitation("in/h", "in", 0.42F)
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
                    testWind(sharedPreferences, "m/s", 10.6F, false)
                }
                and("is in beaufort") {
                    every { sharedPreferences.getString("wind", "m/s") } returns "bf"
                    testWind(sharedPreferences, "bf", 5F, true)
                }
                and("is in km/h") {
                    every { sharedPreferences.getString("wind", "m/s") } returns "km/h"
                    testWind(sharedPreferences, "km/h", 38F, false)
                }
                and("is in mph") {
                    every { sharedPreferences.getString("wind", "m/s") } returns "mph"
                    testWind(sharedPreferences, "mph", 23.6F, false)
                }
                and("is in knots") {
                    every { sharedPreferences.getString("wind", "m/s") } returns "knots"
                    testWind(sharedPreferences, "knots", 20.5F, false)
                }
            }
        }
    }

    afterSpec {
        stopKoin()
    }
})
