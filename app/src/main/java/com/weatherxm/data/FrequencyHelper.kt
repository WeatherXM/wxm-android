package com.weatherxm.data

import android.content.Context
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber


fun otherFrequencies(frequency: Frequency): List<Frequency> {
    return Frequency.values().toMutableList().apply {
        removeIf {
            it == frequency
        }
    }
}

fun countryToFrequency(context: Context, countryCode: String): Frequency? {
    return try {
        val json = JSONObject(
            context.assets.open("countries_frequencies.json").bufferedReader()
                .use { it.readText() })

        if (!json.has(countryCode)) {
            null
        } else {
            val frequencyName = json.getString(countryCode)

            Frequency.values().firstOrNull {
                it.name == frequencyName
            }
        }
    } catch (e: JSONException) {
        Timber.w(e, "Failure: JSON Parsing of countries & frequencies file")
        null
    }
}

fun frequencyToHeliumBleBandValue(frequency: Frequency): Int {
    return when (frequency) {
        Frequency.EU868 -> 5
        Frequency.US915 -> 8
        Frequency.AU915 -> 1
        Frequency.AS923 -> 0
        Frequency.CN470 -> 2
        Frequency.CN779 -> 3
        Frequency.KR920 -> 6
        Frequency.IN865 -> 7
        Frequency.RU864 -> 9
        Frequency.EU433 -> 4
        Frequency.AS923_1 -> 10
        Frequency.AS923_2 -> 11
        Frequency.AS923_3 -> 12
        Frequency.AS923_4 -> 13
    }
}
