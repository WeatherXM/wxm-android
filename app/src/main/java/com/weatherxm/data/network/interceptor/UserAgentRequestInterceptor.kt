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
class UserAgentRequestInterceptor(val context: Context) : Interceptor {

    companion object {
        private const val USER_AGENT = "User-Agent"
    }

    private val packageManager = context.packageManager
    private val userAgent = "${applicationInfo()}; ${systemInfo()}; ${deviceInfo()}"

    private fun applicationInfo(): String {
        return try {
            val packageInfo = packageManager.getPackageInfo(context.packageName, 0)
            val appName = packageInfo.applicationInfo.loadLabel(packageManager)
            val name = packageInfo.versionName
            val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                packageInfo.versionCode
            }
            "(${context.applicationInfo.packageName}) $appName $name ($code)"
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

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        Timber.d("Adding user agent [$userAgent]")
        val requestWithUserAgent = request.newBuilder()
            .header(USER_AGENT, userAgent)
            .build()
        return chain.proceed(requestWithUserAgent)
    }
}
