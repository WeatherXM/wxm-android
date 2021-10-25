package com.weatherxm.util

class Validator {
    companion object {
        const val MINIMUM_PASSWORD_LENGTH = 6
    }

    fun validateUsername(username: String?): Boolean {
        return username.isNullOrEmpty() || !username.contains("@")
    }

    fun validatePassword(password: String?): Boolean {
        return password.isNullOrEmpty() || password.length < MINIMUM_PASSWORD_LENGTH
    }

}
