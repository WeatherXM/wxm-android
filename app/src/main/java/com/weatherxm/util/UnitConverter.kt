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

    fun msToKmh(ms: Float): Float {
        return ms * 3.6F
    }

    fun msToKnots(ms: Float): Float {
        return ms * 1.944F
    }

    fun msToBeaufort(ms: Float): Int {
        return when {
            ms < 0.2 -> 0
            ms < 1.5 -> 1
            ms < 3.3 -> 2
            ms < 5.4 -> 3
            ms < 7.9 -> 4
            ms < 10.7 -> 5
            ms < 13.8 -> 6
            ms < 17.1 -> 7
            ms < 20.7 -> 8
            ms < 24.4 -> 9
            ms < 28.4 -> 10
            ms < 32.6 -> 11
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


