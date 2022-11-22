package com.weatherxm.data

fun countryToFrequency(countryCode: String?): Frequency? {
    if (countryCode.isNullOrEmpty()) return null

    return when (countryCode.uppercase()) {
        "GR" -> Frequency.EU868
        "US" -> Frequency.US915
        else -> null
    }
}

fun otherFrequencies(frequency: Frequency): List<Frequency> {
    return Frequency.values().toMutableList().apply {
        removeIf {
            it == frequency
        }
    }
}
