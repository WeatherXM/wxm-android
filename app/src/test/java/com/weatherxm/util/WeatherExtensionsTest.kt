package com.weatherxm.util

import android.content.SharedPreferences
import com.weatherxm.util.Weather.EMPTY_VALUE
import com.weatherxm.util.Weather.getDecimalsPrecipitation
import com.weatherxm.util.Weather.getDecimalsPressure
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
    functionToInvoke: (Float?) -> String
) {
    and("Unit should be included") {
        then("it should return an $EMPTY_VALUE with the $expectedUnit unit") {
            functionToInvoke(null) shouldBe "$EMPTY_VALUE$expectedUnit"
        }
    }
}

suspend fun BehaviorSpecWhenContainerScope.testNullValue(
    expectedUnit: String,
    functionToInvoke: (Int?) -> String
) {
    and("Unit should be included") {
        then("it should return an $EMPTY_VALUE with the $expectedUnit unit") {
            functionToInvoke(null) shouldBe "$EMPTY_VALUE$expectedUnit"
        }
    }
}

suspend fun BehaviorSpecWhenContainerScope.testNullWindValue() {
    and("Unit should be included") {
        and("wind speed is null") {
            getFormattedWind(null, 1) shouldBe "${EMPTY_VALUE}m/s"
        }
        and("wind direction is null") {
            getFormattedWind(1F, null) shouldBe "${EMPTY_VALUE}m/s"
        }
        and("both wind speed and direction are null") {
            getFormattedWind(null, null) shouldBe "${EMPTY_VALUE}m/s"
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

suspend fun BehaviorSpecWhenContainerScope.testUV(
    resources: Resources,
    value: Int,
    expectedClassification: Int
) {
    then("return the value ($value) with the classification $expectedClassification") {
        getFormattedUV(value) shouldBe "$value${resources.getString(expectedClassification)}"
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
            getFormattedTemperature(25f) shouldBe "$expectedInt$expectedUnit"
        }
        and("One decimal is used") {
            then("it should return the value rounded to one decimal point") {
                getFormattedTemperature(
                    25.48f,
                    decimals = 1
                ) shouldBe "$expectedOneDecimalRounded$expectedUnit"
            }
        }
    }
    and("Short Unit should be included") {
        then("it should return the value with the degrees mark") {
            getFormattedTemperature(25f, fullUnit = false) shouldBe "$expectedInt°"
        }
    }
    and("Unit should NOT be included") {
        then("it should return the value without any unit") {
            getFormattedTemperature(25f, includeUnit = false) shouldBe "$expectedInt"
        }
    }
    and("we should ignore converting the value") {
        then("it should return the value as-is with the correct unit") {
            getFormattedTemperature(
                25f,
                ignoreConversion = true
            ) shouldBe "25$expectedUnit"
        }
    }
}

suspend fun BehaviorSpecWhenContainerScope.testSolarRadiation(expectedValue: Float) {
    then("it should return the value with the correct unit") {
        getFormattedSolarRadiation(25.35f) shouldBe "${expectedValue}W/m2"
    }
    and("unit should NOT be included") {
        then("it should return the value without the unit") {
            getFormattedSolarRadiation(25.35f, false) shouldBe "$expectedValue"
        }
    }
}

suspend fun BehaviorSpecWhenContainerScope.testPressure(
    expectedUnit: String,
    expectedValue: Float
) {
    then("it should return the value with the $expectedUnit unit") {
        getFormattedPressure(1000.35f) shouldBe "$expectedValue$expectedUnit"
    }
    and("unit is NOT included") {
        then("it should return the value without the unit") {
            getFormattedPressure(1000.35f, false) shouldBe "$expectedValue"
        }
    }
    and("we should ignore converting the value") {
        val expectedValueNoConversion = if (getDecimalsPressure() == 1) {
            29.5F
        } else {
            29.54F
        }
        getFormattedPressure(
            29.54f,
            ignoreConversion = true
        ) shouldBe "$expectedValueNoConversion$expectedUnit"
    }
}

suspend fun BehaviorSpecWhenContainerScope.testPrecipitation(
    expectedRateUnit: String,
    expectedAccUnit: String,
    expectedValue: Float
) {
    then("it should return the value with the $expectedRateUnit unit") {
        getFormattedPrecipitation(10.55F) shouldBe "$expectedValue$expectedRateUnit"
    }
    and("$expectedRateUnit is NOT included") {
        then("it should return the value without the unit") {
            getFormattedPrecipitation(10.55F, includeUnit = false) shouldBe "$expectedValue"
        }
    }
    and("it is NOT rain rate") {
        then("it should return the value with the $expectedAccUnit unit") {
            getFormattedPrecipitation(10.55F, false) shouldBe "$expectedValue$expectedAccUnit"
        }
    }
    and("we should ignore converting the value") {
        val expectedValueNoConversion = if (getDecimalsPrecipitation() == 1) {
            10.6F
        } else {
            10.55F
        }
        getFormattedPrecipitation(
            10.55f,
            ignoreConversion = true
        ) shouldBe "$expectedValueNoConversion$expectedRateUnit"
    }
}

suspend fun BehaviorSpecWhenContainerScope.testWind(
    sharedPreferences: SharedPreferences,
    expectedUnit: String,
    expectedValue: Float,
    isBeaufortUsed: Boolean
) {
    val value = if (isBeaufortUsed) expectedValue.toInt() else expectedValue

    and("use Cardinal wind direction") {
        every {
            sharedPreferences.getString("key_wind_direction_preference", "Cardinal")
        } returns "Cardinal"
        then("it should return the value with the $expectedUnit unit") {
            getFormattedWind(10.55F, 10) shouldBe "$value$expectedUnit N"
        }
        and("we should ignore converting the value") {
            val expectedValueNoConversion = if (isBeaufortUsed) 11 else 10.6
            getFormattedWind(
                10.55F,
                10,
                ignoreConversion = true
            ) shouldBe "$expectedValueNoConversion$expectedUnit N"
        }
    }
    and("use Degrees direction") {
        every {
            sharedPreferences.getString("key_wind_direction_preference", "Cardinal")
        } returns "Degrees"
        then("it should return the value with the $expectedUnit unit") {
            getFormattedWind(10.55F, 10) shouldBe "$value$expectedUnit 10°"
        }
    }
    and("units are NOT included") {
        then("it should return the value without the unit") {
            getFormattedWind(10.55F, 10, false) shouldBe "$value"
        }
        and("we should ignore converting the value") {
            val expectedValueNoConversion = if (isBeaufortUsed) 11 else 10.6
            getFormattedWind(
                10.55F,
                10,
                ignoreConversion = true
            ) shouldBe "$expectedValueNoConversion$expectedUnit 10°"
        }
    }
}

