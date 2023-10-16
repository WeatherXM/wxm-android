package com.weatherxm.data.datasource

import android.content.Context
import android.content.Context.TELEPHONY_SERVICE
import android.telephony.TelephonyManager
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.weatherxm.data.CountryInfo
import com.weatherxm.data.Location
import com.weatherxm.data.services.CacheService
import org.json.JSONException
import timber.log.Timber

interface LocationDataSource {
    fun getUserCountry(): String?
    suspend fun getUserCountryLocation(): Location?
}

class LocationDataSourceImpl(
    private val context: Context, private val moshi: Moshi, private val cacheService: CacheService
) : LocationDataSource {

    // https://stackoverflow.com/questions/3659809/where-am-i-get-country
    override fun getUserCountry(): String? {
        val telephonyManager = context.getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        val simCountry = telephonyManager.simCountryIso
        val networkCountry = telephonyManager.networkCountryIso

        return if (simCountry.length == 2) {
            Timber.d("Found user's country via SIM: [country=$simCountry]")
            simCountry.uppercase()
        } else if (networkCountry.length == 2) {
            Timber.d("Found user's country via Network: [country=$networkCountry]")
            networkCountry.uppercase()
        } else {
            null
        }
    }

    override suspend fun getUserCountryLocation(): Location? {
        return getUserCountry()?.let { code ->
            cacheService.getCountriesInfo().ifEmpty {
                parseCountriesJson()
            }?.firstOrNull {
                it.code == code && it.mapCenter != null
            }?.mapCenter
        }
    }

    private fun parseCountriesJson(): List<CountryInfo>? {
        return try {
            val adapter: JsonAdapter<List<CountryInfo>> = moshi.adapter(
                Types.newParameterizedType(List::class.java, CountryInfo::class.java)
            )
            adapter.fromJson(context.assets.open("countries_information.json")
                .bufferedReader().use {
                    it.readText()
                }).apply {
                cacheService.setCountriesInfo(this)
            }
        } catch (e: JSONException) {
            Timber.w(e, "Failure: JSON Parsing of countries information")
            null
        }
    }
}
