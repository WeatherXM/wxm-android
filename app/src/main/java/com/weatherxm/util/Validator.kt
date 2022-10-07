package com.weatherxm.util

import android.util.Patterns

class Validator {
    companion object {
        const val MINIMUM_PASSWORD_LENGTH = 6
        const val ADDRESS_LENGTH = 42
        const val DEV_EUI_KEY_LENGTH = 16
        const val SERIAL_NUMBER_LENGTH = 18
        const val FRIENDLY_NAME_MAX_LENGTH = 64
        const val REGEX_ETH_ADDRESS = "^0x[a-fA-F0-9]{40}\$"
        const val REGEX_SERIAL_NUMBER = "^[a-fA-F0-9]{18}\$"
        const val REGEX_FRIENDLY_NAME = "^(?!\\s*\$).+"
        const val REGEX_DEV_EUI_KEY = "^[a-fA-F0-9]{16}\$"
    }

    fun validateUsername(username: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(username).matches()
    }

    fun validatePassword(password: String): Boolean {
        return password.length >= MINIMUM_PASSWORD_LENGTH
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

    fun validateDevEUI(devEUI: String?): Boolean {
        if (devEUI.isNullOrEmpty() || devEUI.length != DEV_EUI_KEY_LENGTH) {
            return false
        }
        return devEUI.matches(Regex(REGEX_DEV_EUI_KEY))
    }

    fun validateDevKey(devKey: String?): Boolean {
        if (devKey.isNullOrEmpty() || devKey.length != DEV_EUI_KEY_LENGTH) {
            return false
        }
        return devKey.matches(Regex(REGEX_DEV_EUI_KEY))
    }
}
