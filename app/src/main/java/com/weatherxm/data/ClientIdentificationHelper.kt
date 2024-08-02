package com.weatherxm.data

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.weatherxm.data.repository.AppConfigRepository
import timber.log.Timber

class ClientIdentificationHelper(
    private val context: Context,
    private val appConfigRepository: AppConfigRepository
) {

    fun getInterceptorClientIdentifier(): String {
        return "${applicationInfo()}; ${androidInfo()}; ${deviceInfo()}"
    }

    private fun applicationInfo(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val name = packageInfo.versionName
            val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                packageInfo.versionCode
            }
            val installationId = appConfigRepository.getInstallationId() ?: "N/A"
            "wxm-android (${context.applicationInfo.packageName}); $name ($code); $installationId"
        } catch (e: PackageManager.NameNotFoundException) {
            Timber.d("Could not resolve application info: $e")
            "${context.applicationInfo.packageName} N/A (N/A)"
        }
    }

    private fun androidInfo(): String {
        return "Android: ${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE})"
    }

    private fun deviceInfo(): String {
        return "Device: ${Build.MANUFACTURER} (${Build.MODEL})"
    }
}
