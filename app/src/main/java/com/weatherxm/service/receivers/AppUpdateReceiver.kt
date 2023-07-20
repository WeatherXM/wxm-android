package com.weatherxm.service.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.weatherxm.ui.widgets.currentweather.CurrentWeatherWidgetWorkerUpdate
import org.koin.core.component.KoinComponent

class AppUpdateReceiver : BroadcastReceiver(), KoinComponent {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            CurrentWeatherWidgetWorkerUpdate.restartAllWorkers(context)
        }
    }
}
