package com.weatherxm.util

import com.weatherxm.R
import com.weatherxm.TestConfig.resources
import com.weatherxm.TestConfig.sharedPref
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
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest

class WeatherTest : KoinTest, BehaviorSpec({

    beforeSpec {
        startKoin {
            modules(
                module {
                    single { resources }
                    single { sharedPref }
                }
            )
        }
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
                    every { sharedPref.getString("temperature_unit", "°C") } returns "°F"
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
                    every { sharedPref.getString("key_pressure_preference", "hPa") } returns "inHg"
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
                    every { sharedPref.getString("precipitation_unit", "mm") } returns "in"
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
                    testWind(sharedPref, "m/s", 10.6F, false)
                }
                and("is in beaufort") {
                    every { sharedPref.getString("wind_speed_unit", "m/s") } returns "bf"
                    testWind(sharedPref, "bf", 5F, true)
                }
                and("is in km/h") {
                    every { sharedPref.getString("wind_speed_unit", "m/s") } returns "km/h"
                    testWind(sharedPref, "km/h", 38F, false)
                }
                and("is in mph") {
                    every { sharedPref.getString("wind_speed_unit", "m/s") } returns "mph"
                    testWind(sharedPref, "mph", 23.6F, false)
                }
                and("is in knots") {
                    every { sharedPref.getString("wind_speed_unit", "m/s") } returns "knots"
                    testWind(sharedPref, "knots", 20.5F, false)
                }
            }
        }
    }

    afterSpec {
        stopKoin()
    }
})
