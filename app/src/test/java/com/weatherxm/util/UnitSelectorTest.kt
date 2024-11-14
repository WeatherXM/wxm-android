package com.weatherxm.util

import com.weatherxm.R
import com.weatherxm.TestConfig.cacheService
import com.weatherxm.TestConfig.context
import com.weatherxm.data.services.CacheService
import com.weatherxm.data.services.CacheService.Companion.KEY_PRECIP
import com.weatherxm.data.services.CacheService.Companion.KEY_PRESSURE
import com.weatherxm.data.services.CacheService.Companion.KEY_TEMPERATURE
import com.weatherxm.data.services.CacheService.Companion.KEY_WIND
import com.weatherxm.data.services.CacheService.Companion.KEY_WIND_DIR
import com.weatherxm.ui.common.WeatherUnit
import com.weatherxm.ui.common.WeatherUnitType
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest

class UnitSelectorTest : KoinTest, BehaviorSpec({
    beforeSpec {
        startKoin {
            modules(
                module {
                    single<CacheService> {
                        cacheService
                    }
                }
            )
        }
    }

    context("Get Temperature Unit") {
        When("it's celsius") {
            every {
                cacheService.getPreferredUnit(
                    unitKeyResId = KEY_TEMPERATURE,
                    defaultUnitResId = R.string.temperature_celsius
                )
            } returns context.getString(R.string.temperature_celsius)
            then("return WeatherUnit with the associated type and unit") {
                UnitSelector.getTemperatureUnit(context) shouldBe WeatherUnit(
                    WeatherUnitType.CELSIUS,
                    context.getString(R.string.temperature_celsius)
                )
            }
        }
        When("it's fahrenheit") {
            every {
                cacheService.getPreferredUnit(
                    unitKeyResId = KEY_TEMPERATURE,
                    defaultUnitResId = R.string.temperature_celsius
                )
            } returns context.getString(R.string.temperature_fahrenheit)
            then("return WeatherUnit with the associated type and unit") {
                UnitSelector.getTemperatureUnit(context) shouldBe WeatherUnit(
                    WeatherUnitType.FAHRENHEIT,
                    context.getString(R.string.temperature_fahrenheit)
                )
            }
        }
    }

    context("Get Precipitation Unit") {
        When("it's millimeters") {
            and("it's rain rate") {
                every {
                    cacheService.getPreferredUnit(
                        unitKeyResId = KEY_PRECIP,
                        defaultUnitResId = R.string.precipitation_mm
                    )
                } returns context.getString(R.string.precipitation_mm)
                then("return WeatherUnit with the associated type and unit") {
                    UnitSelector.getPrecipitationUnit(context, true) shouldBe WeatherUnit(
                        WeatherUnitType.MILLIMETERS,
                        context.getString(R.string.precipitation_mm_hour)
                    )
                }
            }
            and("it's NOT rain rate") {
                every {
                    cacheService.getPreferredUnit(
                        unitKeyResId = KEY_PRECIP,
                        defaultUnitResId = R.string.precipitation_mm
                    )
                } returns context.getString(R.string.precipitation_mm)
                then("return WeatherUnit with the associated type and unit") {
                    UnitSelector.getPrecipitationUnit(context, false) shouldBe WeatherUnit(
                        WeatherUnitType.MILLIMETERS,
                        context.getString(R.string.precipitation_mm)
                    )
                }
            }
        }
        When("it's inches") {
            and("it's rain rate") {
                every {
                    cacheService.getPreferredUnit(
                        unitKeyResId = KEY_PRECIP,
                        defaultUnitResId = R.string.precipitation_mm
                    )
                } returns context.getString(R.string.precipitation_in)
                then("return WeatherUnit with the associated type and unit") {
                    UnitSelector.getPrecipitationUnit(context, true) shouldBe WeatherUnit(
                        WeatherUnitType.INCHES,
                        context.getString(R.string.precipitation_in_hour)
                    )
                }
            }
            and("it's NOT rain rate") {
                every {
                    cacheService.getPreferredUnit(
                        unitKeyResId = KEY_PRECIP,
                        defaultUnitResId = R.string.precipitation_mm
                    )
                } returns context.getString(R.string.precipitation_in)
                then("return WeatherUnit with the associated type and unit") {
                    UnitSelector.getPrecipitationUnit(context, false) shouldBe WeatherUnit(
                        WeatherUnitType.INCHES,
                        context.getString(R.string.precipitation_in)
                    )
                }
            }
        }
    }

    context("Get Pressure Unit") {
        When("it's hPa") {
            every {
                cacheService.getPreferredUnit(
                    unitKeyResId = KEY_PRESSURE,
                    defaultUnitResId = R.string.pressure_hpa
                )
            } returns context.getString(R.string.pressure_hpa)
            then("return WeatherUnit with the associated type and unit") {
                UnitSelector.getPressureUnit(context) shouldBe WeatherUnit(
                    WeatherUnitType.HPA,
                    context.getString(R.string.pressure_hpa)
                )
            }
        }
        When("it's inHg") {
            every {
                cacheService.getPreferredUnit(
                    unitKeyResId = KEY_PRESSURE,
                    defaultUnitResId = R.string.pressure_hpa
                )
            } returns context.getString(R.string.pressure_inHg)
            then("return WeatherUnit with the associated type and unit") {
                UnitSelector.getPressureUnit(context) shouldBe WeatherUnit(
                    WeatherUnitType.INHG,
                    context.getString(R.string.pressure_inHg)
                )
            }
        }
    }

    context("Get Wind") {
        When("it's m/s") {
            every {
                cacheService.getPreferredUnit(
                    unitKeyResId = KEY_WIND,
                    defaultUnitResId = R.string.wind_speed_ms
                )
            } returns context.getString(R.string.wind_speed_ms)
            then("return WeatherUnit with the associated type and unit") {
                UnitSelector.getWindUnit(context) shouldBe WeatherUnit(
                    WeatherUnitType.MS,
                    context.getString(R.string.wind_speed_ms)
                )
            }
        }
        When("it's bf") {
            every {
                cacheService.getPreferredUnit(
                    unitKeyResId = KEY_WIND,
                    defaultUnitResId = R.string.wind_speed_ms
                )
            } returns context.getString(R.string.wind_speed_beaufort)
            then("return WeatherUnit with the associated type and unit") {
                UnitSelector.getWindUnit(context) shouldBe WeatherUnit(
                    WeatherUnitType.BEAUFORT,
                    context.getString(R.string.wind_speed_beaufort)
                )
            }
        }
        When("it's km/h") {
            every {
                cacheService.getPreferredUnit(
                    unitKeyResId = KEY_WIND,
                    defaultUnitResId = R.string.wind_speed_ms
                )
            } returns context.getString(R.string.wind_speed_kmh)
            then("return WeatherUnit with the associated type and unit") {
                UnitSelector.getWindUnit(context) shouldBe WeatherUnit(
                    WeatherUnitType.KMH,
                    context.getString(R.string.wind_speed_kmh)
                )
            }
        }
        When("it's mph") {
            every {
                cacheService.getPreferredUnit(
                    unitKeyResId = KEY_WIND,
                    defaultUnitResId = R.string.wind_speed_ms
                )
            } returns context.getString(R.string.wind_speed_mph)
            then("return WeatherUnit with the associated type and unit") {
                UnitSelector.getWindUnit(context) shouldBe WeatherUnit(
                    WeatherUnitType.MPH,
                    context.getString(R.string.wind_speed_mph)
                )
            }
        }
        When("it's knots") {
            every {
                cacheService.getPreferredUnit(
                    unitKeyResId = KEY_WIND,
                    defaultUnitResId = R.string.wind_speed_ms
                )
            } returns context.getString(R.string.wind_speed_knots)
            then("return WeatherUnit with the associated type and unit") {
                UnitSelector.getWindUnit(context) shouldBe WeatherUnit(
                    WeatherUnitType.KNOTS,
                    context.getString(R.string.wind_speed_knots)
                )
            }
        }
    }

    context("Get Wind Direction Unit") {
        When("it's Cardinal") {
            every {
                cacheService.getPreferredUnit(
                    unitKeyResId = KEY_WIND_DIR,
                    defaultUnitResId = R.string.wind_direction_cardinal
                )
            } returns context.getString(R.string.wind_direction_cardinal)
            then("return WeatherUnit with the associated type and unit") {
                UnitSelector.getWindDirectionUnit(context) shouldBe WeatherUnit(
                    WeatherUnitType.CARDINAL,
                    context.getString(R.string.wind_direction_cardinal)
                )
            }
        }
        When("it's Degrees") {
            every {
                cacheService.getPreferredUnit(
                    unitKeyResId = KEY_WIND_DIR,
                    defaultUnitResId = R.string.wind_direction_cardinal
                )
            } returns context.getString(R.string.wind_direction_degrees)
            then("return WeatherUnit with the associated type and unit") {
                UnitSelector.getWindDirectionUnit(context) shouldBe WeatherUnit(
                    WeatherUnitType.DEGREES,
                    context.getString(R.string.wind_direction_degrees)
                )
            }
        }
    }


    afterSpec {
        stopKoin()
    }
})
