package com.weatherxm.data.network.interceptor

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber

/**
 * {@see okhttp3.Interceptor} that adds User-Agent header to the request,
 * using app & device information.
 */
class ClientIdentificationRequestInterceptor(val context: Context) : Interceptor {

    companion object {
        private const val CLIENT_IDENTIFICATION_HEADER = "X-WXM-Client"
    }

    private val packageManager = context.packageManager
    private val clientIdentifier = "${applicationInfo()}; ${systemInfo()}; ${deviceInfo()}"

    private fun applicationInfo(): String {
        return try {
            val packageInfo = packageManager.getPackageInfo(context.packageName, 0)
            val name = packageInfo.versionName
            val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                packageInfo.versionCode
            }
            "wxm-android (${context.applicationInfo.packageName}); $name ($code)"
        } catch (e: PackageManager.NameNotFoundException) {
            Timber.d("Could not resolve application info: $e")
            "${context.applicationInfo.packageName} N/A (N/A)"
        }
    }

    private fun systemInfo(): String {
        return "Android ${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE})"
    }

    private fun deviceInfo(): String {
        return "${Build.MANUFACTURER} (${Build.MODEL})"
    }

    fun getClientIdentifier(): String {
        return clientIdentifier
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        Timber.d("Adding client identification header [$clientIdentifier]")
        val requestWithUserAgent = request.newBuilder()
            .header(CLIENT_IDENTIFICATION_HEADER, clientIdentifier)
            .build()
        return chain.proceed(requestWithUserAgent)
    }
}
