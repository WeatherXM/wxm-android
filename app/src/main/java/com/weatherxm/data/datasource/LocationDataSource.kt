package com.weatherxm.data.datasource

import android.content.Context
import android.content.Context.TELEPHONY_SERVICE
import android.telephony.TelephonyManager
import timber.log.Timber


interface LocationDataSource {
    fun getUserCountry(): String?
}

class LocationDataSourceImpl(private val context: Context) : LocationDataSource {

    // https://stackoverflow.com/questions/3659809/where-am-i-get-country
    override fun getUserCountry(): String? {
        val telephonyManager = context.getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        val simCountry = telephonyManager.simCountryIso
        val networkCountry = telephonyManager.networkCountryIso

        return if (simCountry.length == 2) {
            Timber.d("Found user's country via SIM: [country=$simCountry]")
            simCountry
        } else {
            if (networkCountry.length == 2) {
                Timber.d("Found user's country via Network: [country=$networkCountry]")
                networkCountry
            } else {
                null
            }
        }
    }
}
