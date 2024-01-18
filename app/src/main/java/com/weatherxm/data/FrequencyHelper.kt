package com.weatherxm.data

import android.content.Context
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.json.JSONException
import timber.log.Timber

fun otherFrequencies(frequency: Frequency): List<Frequency> {
    return Frequency.values().toMutableList().apply {
        removeIf {
            it == frequency
        }
    }
}

fun countryToFrequency(context: Context, countryCode: String, moshi: Moshi): Frequency? {
    return try {
        val adapter: JsonAdapter<List<CountryInfo>> =
            moshi.adapter(Types.newParameterizedType(List::class.java, CountryInfo::class.java))

        val frequencyName = adapter
            .fromJson(
                context.assets.open("countries_information.json").bufferedReader().use {
                    it.readText()
                })
            ?.firstOrNull {
                it.code == countryCode
            }
            ?.heliumFrequency

        Frequency.values().firstOrNull {
            it.name == frequencyName
        }
    } catch (e: JSONException) {
        Timber.w(e, "Failure: JSON Parsing of countries information")
        null
    }
}

@Suppress("MagicNumber", "ComplexMethod")
fun frequencyToHeliumBleBandValue(frequency: Frequency): Int {
    return when (frequency) {
        Frequency.EU868 -> 5
        Frequency.US915 -> 8
        Frequency.AU915 -> 1
        Frequency.CN470 -> 2
        Frequency.KR920 -> 6
        Frequency.IN865 -> 7
        Frequency.RU864 -> 9
        Frequency.AS923_1 -> 10
        Frequency.AS923_2 -> 11
        Frequency.AS923_3 -> 12
        Frequency.AS923_4 -> 13
    }
}
