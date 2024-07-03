package com.weatherxm.util

import com.weatherxm.util.UnitConverter.celsiusToFahrenheit
import com.weatherxm.util.UnitConverter.degreesToCardinal
import com.weatherxm.util.UnitConverter.hpaToInHg
import com.weatherxm.util.UnitConverter.millimetersToInches
import com.weatherxm.util.UnitConverter.msToBeaufort
import com.weatherxm.util.UnitConverter.msToKmh
import com.weatherxm.util.UnitConverter.msToKnots
import com.weatherxm.util.UnitConverter.msToMph
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class UnitConverterTest : BehaviorSpec({
    context("Conversion between Weather Units") {
        given("a value in Celsius") {
            then("the correct Fahrenheit value should be returned") {
                celsiusToFahrenheit(-5F) shouldBe 23F
                celsiusToFahrenheit(0F) shouldBe 32F
                celsiusToFahrenheit(10F) shouldBe 50F
            }
        }
        given("a value in Millimeters") {
            then("the correct Inches value should be returned") {
                millimetersToInches(0.12F) shouldBe 0.0047244094F
                millimetersToInches(1F) shouldBe 0.03937008F
                millimetersToInches(5.12F) shouldBe 0.2015748F
            }
        }
        given("a value in hPa") {
            then("the correct inHg value should be returned") {
                hpaToInHg(995F) shouldBe 29.382349F
                hpaToInHg(1000F) shouldBe 29.53F
                hpaToInHg(1010F) shouldBe 29.8253F
            }
        }
        given("a value in m/s") {
            then("the correct km/h value should be returned") {
                msToKmh(0.51F) shouldBe 1.836F
                msToKmh(1F) shouldBe 3.6F
                msToKmh(5.3F) shouldBe 19.08F
            }
            then("the correct mph value should be returned") {
                msToMph(0.52F) shouldBe 1.16324F
                msToMph(1F) shouldBe 2.237F
                msToMph(5.3F) shouldBe 11.8561F
            }
            then("the correct Beaufort value should be returned") {
                msToBeaufort(0.1F) shouldBe 0
                msToBeaufort(1F) shouldBe 1
                msToBeaufort(3F) shouldBe 2
                msToBeaufort(5F) shouldBe 3
                msToBeaufort(7F) shouldBe 4
                msToBeaufort(10F) shouldBe 5
                msToBeaufort(13F) shouldBe 6
                msToBeaufort(17F) shouldBe 7
                msToBeaufort(18F) shouldBe 8
                msToBeaufort(23F) shouldBe 9
                msToBeaufort(27F) shouldBe 10
                msToBeaufort(31F) shouldBe 11
                msToBeaufort(32.7F) shouldBe 12
            }
            then("the correct knots value should be returned") {
                msToKnots(0.52F) shouldBe 1.01088F
                msToKnots(1F) shouldBe 1.944F
                msToKnots(5.3F) shouldBe 10.303201F
            }
        }
        given("a value in degrees") {
            then("the correct Cardinal value") {
                degreesToCardinal(-10) shouldBe "N"
                degreesToCardinal(0) shouldBe "N"
                degreesToCardinal(22) shouldBe "NNE"
                degreesToCardinal(45) shouldBe "NE"
                degreesToCardinal(67) shouldBe "ENE"
                degreesToCardinal(112) shouldBe "ESE"
                degreesToCardinal(135) shouldBe "SE"
                degreesToCardinal(157) shouldBe "SSE"
                degreesToCardinal(180) shouldBe "S"
                degreesToCardinal(202) shouldBe "SSW"
                degreesToCardinal(225) shouldBe "SW"
                degreesToCardinal(247) shouldBe "WSW"
                degreesToCardinal(292) shouldBe "WNW"
                degreesToCardinal(315) shouldBe "NW"
                degreesToCardinal(360) shouldBe "N"
            }
        }
    }
})
