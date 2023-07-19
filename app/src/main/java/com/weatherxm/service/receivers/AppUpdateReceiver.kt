package com.weatherxm.service.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.preference.PreferenceManager
import com.weatherxm.data.services.CacheService
import com.weatherxm.data.services.CacheService.Companion.KEY_CURRENT_WEATHER_WIDGET_IDS
import com.weatherxm.ui.widgets.currentweather.CurrentWeatherWidgetWorkerUpdate
import org.koin.core.component.KoinComponent

class AppUpdateReceiver : BroadcastReceiver(), KoinComponent {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val ids = prefs.getStringSet(KEY_CURRENT_WEATHER_WIDGET_IDS, setOf())

            ids?.forEach {
                val deviceId = prefs.getString(CacheService.getWidgetFormattedKey(it.toInt()), "")
                if (!deviceId.isNullOrEmpty()) {
                    CurrentWeatherWidgetWorkerUpdate.initAndStart(context, it.toInt(), deviceId)
                }
            }
        }
    }
}
