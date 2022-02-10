package com.weatherxm.util

class Validator {
    companion object {
        const val MINIMUM_PASSWORD_LENGTH = 6
        const val MINIMUM_ADDRESS_LENGTH = 40
        const val SERIAL_NUMBER_LENGTH = 18
        const val REGEX_ETH_ADDRESS = "^0x[a-fA-F0-9]{40}\$"
        const val REGEX_SERIAL_NUMBER = "^[a-fA-F0-9]{18}\$"
    }

    fun validateUsername(username: String?): Boolean {
        return username.isNullOrEmpty() || !username.contains("@")
    }

    fun validatePassword(password: String?): Boolean {
        return password.isNullOrEmpty() || password.length < MINIMUM_PASSWORD_LENGTH
    }

    fun validateEthAddress(address: String?): Boolean {
        if (address.isNullOrEmpty() || address.length < MINIMUM_ADDRESS_LENGTH) {
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
}
