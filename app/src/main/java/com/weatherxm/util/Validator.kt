package com.weatherxm.util

import android.util.Patterns
import com.weatherxm.data.Location

@Suppress("MagicNumber")
object Validator {
    private const val MINIMUM_PASSWORD_LENGTH = 6
    private const val ADDRESS_LENGTH = 42
    private const val SERIAL_NUMBER_LENGTH = 18
    private const val FRIENDLY_NAME_MAX_LENGTH = 64
    private const val REGEX_ETH_ADDRESS = "^0x[a-fA-F0-9]{40}\$"
    private const val REGEX_SERIAL_NUMBER = "^[a-fA-F0-9]{18}\$"
    private const val REGEX_FRIENDLY_NAME = "^(?!\\s*\$).+"
    private val LATITUDE_BOUNDS = -90.0..90.0
    private val LONGITUDE_BOUNDS = -180.0..180.0
    private val EMPTY_LOCATION = Location(0.0, 0.0)
    private const val MINIMUM_NETWORK_SEARCH_QUERY = 2

    fun validateUsername(username: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(username).matches()
    }

    fun validatePassword(password: String): Boolean {
        return password.length >= MINIMUM_PASSWORD_LENGTH
    }

    fun validateNetworkSearchQuery(query: String): Boolean {
        return query.trim().length >= MINIMUM_NETWORK_SEARCH_QUERY
    }

    fun validateLocation(lat: Double, lon: Double): Boolean {
        return lat in LATITUDE_BOUNDS && lon in LONGITUDE_BOUNDS
            && Location(lat, lon) != EMPTY_LOCATION
    }

    fun validateEthAddress(address: String?): Boolean {
        if (address.isNullOrEmpty() || address.length != ADDRESS_LENGTH) {
            return false
        }
        return address.matches(Regex(REGEX_ETH_ADDRESS))
    }

    fun validateSerialNumber(serialNumber: String?): Boolean {
        if (serialNumber.isNullOrEmpty() || serialNumber.length != SERIAL_NUMBER_LENGTH) {
            return false
        }
        return serialNumber.matches(Regex(REGEX_SERIAL_NUMBER))
    }

    fun validateFriendlyName(friendlyName: String?): Boolean {
        if (friendlyName.isNullOrEmpty() || friendlyName.length > FRIENDLY_NAME_MAX_LENGTH) {
            return false
        }
        return friendlyName.matches(Regex(REGEX_FRIENDLY_NAME))
    }
}
