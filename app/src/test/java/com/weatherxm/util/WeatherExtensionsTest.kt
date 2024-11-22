package com.weatherxm.util

import android.content.Context
import com.weatherxm.R
import com.weatherxm.TestConfig.context
import com.weatherxm.ui.common.Contracts.EMPTY_VALUE
import com.weatherxm.ui.common.WeatherUnit
import com.weatherxm.ui.common.WeatherUnitType
import com.weatherxm.util.Weather.getFormattedPrecipitation
import com.weatherxm.util.Weather.getFormattedPressure
import com.weatherxm.util.Weather.getFormattedSolarRadiation
import com.weatherxm.util.Weather.getFormattedTemperature
import com.weatherxm.util.Weather.getFormattedUV
import com.weatherxm.util.Weather.getFormattedWind
import com.weatherxm.util.Weather.getWeatherAnimation
import com.weatherxm.util.Weather.getWeatherStaticIcon
import io.kotest.core.spec.style.scopes.BehaviorSpecContextContainerScope
import io.kotest.core.spec.style.scopes.BehaviorSpecGivenContainerScope
import io.kotest.core.spec.style.scopes.BehaviorSpecWhenContainerScope
import io.kotest.matchers.shouldBe
import io.mockk.every


suspend fun BehaviorSpecWhenContainerScope.testNullFloatValue(
    expectedUnit: String,
    functionToInvoke: (Context, Float?) -> String
) {
    and("Unit should be included") {
        then("it should return an $EMPTY_VALUE with the $expectedUnit unit") {
            functionToInvoke(context, null) shouldBe "$EMPTY_VALUE$expectedUnit"
        }
    }
}

suspend fun BehaviorSpecWhenContainerScope.testNullValue(
    expectedUnit: String,
    functionToInvoke: (Context, Int?) -> String
) {
    and("Unit should be included") {
        then("it should return an $EMPTY_VALUE with the $expectedUnit unit") {
            functionToInvoke(context, null) shouldBe "$EMPTY_VALUE$expectedUnit"
        }
    }
}

suspend fun BehaviorSpecWhenContainerScope.testNullWindValue() {
    and("Unit should be included") {
        and("wind speed is null") {
            getFormattedWind(context, null, 1) shouldBe "${EMPTY_VALUE}m/s"
        }
        and("wind direction is null") {
            getFormattedWind(context, 1F, null) shouldBe "${EMPTY_VALUE}m/s"
        }
        and("both wind speed and direction are null") {
            getFormattedWind(context, null, null) shouldBe "${EMPTY_VALUE}m/s"
        }
    }
}

suspend fun BehaviorSpecGivenContainerScope.testWeatherIcon(
    icon: String?,
    expectedAnimation: Int,
    expectedStaticIcon: Int?
) {
    When("$icon") {
        then("Get the animation") {
            getWeatherAnimation(icon) shouldBe expectedAnimation
        }
        then("Get the static icon") {
            getWeatherStaticIcon(icon) shouldBe expectedStaticIcon
        }
    }
}

suspend fun BehaviorSpecWhenContainerScope.testUV(value: Int, expectedClassification: Int) {
    then("return the value ($value) with the classification $expectedClassification") {
        getFormattedUV(context, value) shouldBe "$value${context.getString(expectedClassification)}"
    }
}

suspend fun BehaviorSpecContextContainerScope.testPercentageMeasurements(
    name: String,
    value: Int?,
    functionToInvoke: (Int?, Boolean) -> String
) {
    given("A $name value") {
        When("value is null") {
            then("it should return an $EMPTY_VALUE with the %") {
                functionToInvoke(null, true) shouldBe "$EMPTY_VALUE%"
            }
            and("unit should not be included") {
                then("it should return an $EMPTY_VALUE") {
                    functionToInvoke(null, false) shouldBe EMPTY_VALUE
                }
            }
        }
        When("value is not null") {
            then("it should return the value with the %") {
                functionToInvoke(value, true) shouldBe "50%"
            }
            and("unit should not be included") {
                then("it should return the value without the %") {
                    functionToInvoke(value, false) shouldBe "50"
                }
            }
        }
    }
}

suspend fun BehaviorSpecWhenContainerScope.testTemperature(
    expectedUnit: String,
    expectedInt: Int,
    expectedOneDecimalRounded: Float,
) {
    and("Full Unit should be included") {
        then("it should return the value with the correct unit") {
            getFormattedTemperature(context, 25f) shouldBe "$expectedInt$expectedUnit"
        }
        and("One decimal is used") {
            then("it should return the value rounded to one decimal point") {
                getFormattedTemperature(
                    context,
                    25.48f,
                    decimals = 1
                ) shouldBe "$expectedOneDecimalRounded$expectedUnit"
            }
        }
    }
    and("Short Unit should be included") {
        then("it should return the value with the degrees mark") {
            getFormattedTemperature(context, 25f, fullUnit = false) shouldBe "$expectedInt°"
        }
    }
    and("Unit should NOT be included") {
        then("it should return the value without any unit") {
            getFormattedTemperature(context, 25f, includeUnit = false) shouldBe "$expectedInt"
        }
    }
}

suspend fun BehaviorSpecWhenContainerScope.testSolarRadiation(expectedValue: Float) {
    then("it should return the value with the correct unit") {
        getFormattedSolarRadiation(context, 25.35f) shouldBe "${expectedValue}W/m2"
    }
    and("unit should NOT be included") {
        then("it should return the value without the unit") {
            getFormattedSolarRadiation(context, 25.35f, false) shouldBe "$expectedValue"
        }
    }
}

suspend fun BehaviorSpecWhenContainerScope.testPressure(
    weatherUnit: WeatherUnit,
    expectedValue: String
) {
    then("it should return the value with the ${weatherUnit.unit} unit") {
        getFormattedPressure(context, 1000.35f) shouldBe "$expectedValue${weatherUnit.unit}"
    }
    and("unit is NOT included") {
        then("it should return the value without the unit") {
            getFormattedPressure(context, 1000.35f, false) shouldBe expectedValue
        }
    }
}

suspend fun BehaviorSpecWhenContainerScope.testPrecipitation(
    expectedRateUnit: String,
    expectedAccUnit: String,
    expectedValue: Float
) {
    then("it should return the value with the $expectedRateUnit unit") {
        getFormattedPrecipitation(context, 10.55F) shouldBe "$expectedValue$expectedRateUnit"
    }
    and("$expectedRateUnit is NOT included") {
        then("it should return the value without the unit") {
            getFormattedPrecipitation(
                context,
                10.55F,
                includeUnit = false
            ) shouldBe "$expectedValue"
        }
    }
    and("it is NOT rain rate") {
        then("it should return the value with the $expectedAccUnit unit") {
            getFormattedPrecipitation(
                context,
                10.55F,
                false
            ) shouldBe "$expectedValue$expectedAccUnit"
        }
    }
}

suspend fun BehaviorSpecWhenContainerScope.testWind(
    expectedUnit: String,
    expectedValue: Float,
    isBeaufortUsed: Boolean
) {
    val value = if (isBeaufortUsed) expectedValue.toInt() else expectedValue

    and("use Cardinal wind direction") {
        every { UnitSelector.getWindDirectionUnit(context) } returns WeatherUnit(
            WeatherUnitType.CARDINAL,
            context.getString(R.string.wind_direction_cardinal)
        )
        then("it should return the value with the $expectedUnit unit") {
            getFormattedWind(context, 10.55F, 10) shouldBe "$value$expectedUnit N"
        }
    }
    and("use Degrees direction") {
        every { UnitSelector.getWindDirectionUnit(context) } returns WeatherUnit(
            WeatherUnitType.DEGREES,
            context.getString(R.string.wind_direction_degrees)
        )
        then("it should return the value with the $expectedUnit unit") {
            getFormattedWind(context, 10.55F, 10) shouldBe "$value$expectedUnit 10°"
        }
    }
    and("units are NOT included") {
        then("it should return the value without the unit") {
            getFormattedWind(context, 10.55F, 10, false) shouldBe "$value"
        }
    }
}

