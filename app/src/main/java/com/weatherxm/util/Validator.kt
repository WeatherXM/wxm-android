package com.weatherxm.util

import android.util.Patterns
import com.weatherxm.data.Location
import com.weatherxm.ui.common.DeviceType

@Suppress("MagicNumber")
object Validator {
    private const val MINIMUM_PASSWORD_LENGTH = 6
    private const val ADDRESS_LENGTH = 42
    private const val FRIENDLY_NAME_MAX_LENGTH = 24
    private const val REGEX_ETH_ADDRESS = "^0x[a-fA-F0-9]{40}\$"
    private const val REGEX_M5_SERIAL_NUMBER = "^[a-fA-F0-9]{18}\$"
    private const val REGEX_D1_SERIAL_NUMBER = "^[a-fA-F0-9]{20}\$"
    private const val REGEX_PULSE_SERIAL_NUMBER = "^[a-fA-F0-9]{16}\$"
    private const val REGEX_CLAIMING_KEY = "^[0-9]{6}\$"
    private const val REGEX_FRIENDLY_NAME = "^\\S.{0,24}$"
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

    fun validateSerialNumber(serialNumber: String, deviceType: DeviceType): Boolean {
        return when (deviceType) {
            DeviceType.M5_WIFI -> serialNumber.matches(Regex(REGEX_M5_SERIAL_NUMBER))
            DeviceType.D1_WIFI -> serialNumber.matches(Regex(REGEX_D1_SERIAL_NUMBER))
            DeviceType.PULSE_4G -> serialNumber.matches(Regex(REGEX_PULSE_SERIAL_NUMBER))
            else -> false
        }
    }

    fun validateClaimingKey(claimingKey: String): Boolean {
        return claimingKey.matches(Regex(REGEX_CLAIMING_KEY))
    }

    fun validateFriendlyName(friendlyName: String?): Boolean {
        if (friendlyName.isNullOrEmpty() || friendlyName.length > FRIENDLY_NAME_MAX_LENGTH) {
            return false
        }
        return friendlyName.matches(Regex(REGEX_FRIENDLY_NAME))
    }
}
