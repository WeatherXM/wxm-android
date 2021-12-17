package com.weatherxm.util

class Validator {
    companion object {
        const val MINIMUM_PASSWORD_LENGTH = 6
        const val MINIMUM_ADDRESS_LENGTH = 40
        const val REGEX_ETH_ADDRESS = "^0x[a-fA-F0-9]{40}\$"
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
}
