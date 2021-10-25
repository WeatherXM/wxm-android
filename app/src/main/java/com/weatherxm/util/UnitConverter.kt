package com.weatherxm.util

import kotlin.math.floor

@Suppress("MagicNumber")
object UnitConverter {

    fun celsiusToFahrenheit(celsius: Float): Float {
        return celsius * 9 / 5 + 32
    }

    fun millimetersToInches(mm: Float): Float {
        return mm / 25.4F
    }

    fun hpaToInHg(hpa: Float): Float {
        return hpa * 0.0295F
    }

    fun kmhToMs(kmh: Float): Float {
        return kmh / 3.6F
    }

    fun kmhToKnots(kmh: Float): Float {
        return kmh / 1.852F
    }

    fun kmhToBeaufort(kmh: Int): Int {
        return when {
            kmh < 2 -> 0
            kmh < 6 -> 1
            kmh < 12 -> 2
            kmh < 20 -> 3
            kmh < 29 -> 4
            kmh < 39 -> 5
            kmh < 50 -> 6
            kmh < 62 -> 7
            kmh < 75 -> 8
            kmh < 89 -> 9
            kmh < 103 -> 10
            kmh < 118 -> 11
            else -> 12
        }
    }

    fun degreesToCardinal(value: Int): String {
        val normalized = floor((value / 22.5) + 0.5).toInt()
        val cardinal = listOf(
            "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
            "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"
        )
        return cardinal[normalized.mod(16)]
    }
}


