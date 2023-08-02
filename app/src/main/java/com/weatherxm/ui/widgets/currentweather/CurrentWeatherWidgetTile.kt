package com.weatherxm.ui.widgets.currentweather

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_CANCEL_CURRENT
import android.app.PendingIntent.FLAG_MUTABLE
import android.app.TaskStackBuilder
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
import android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID
import android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import com.weatherxm.R
import com.weatherxm.data.DeviceProfile
import com.weatherxm.data.services.CacheService
import com.weatherxm.ui.common.Contracts
import com.weatherxm.ui.common.Contracts.ARG_IS_CUSTOM_APPWIDGET_UPDATE
import com.weatherxm.ui.common.Contracts.ARG_WIDGET_TYPE
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.devicedetails.DeviceDetailsActivity
import com.weatherxm.ui.login.LoginActivity
import com.weatherxm.ui.widgets.WidgetType
import com.weatherxm.usecases.WidgetCurrentWeatherUseCase
import com.weatherxm.util.DateTimeHelper.getFormattedTime
import com.weatherxm.util.DateTimeHelper.getRelativeFormattedTime
import com.weatherxm.util.Weather
import com.weatherxm.util.WidgetHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Implementation of Current Weather Widget Tile functionality.
 */
class CurrentWeatherWidgetTile : AppWidgetProvider(), KoinComponent {
    private val usecase: WidgetCurrentWeatherUseCase by inject()
    private val widgetHelper: WidgetHelper by inject()

    @Suppress("MagicNumber")
    override fun onReceive(context: Context, intent: Intent?) {
        super.onReceive(context, intent)

        // These variables are useful for identifying what type of update to do
        val extras = intent?.extras
        val appWidgetId =
            extras?.getInt(EXTRA_APPWIDGET_ID, INVALID_APPWIDGET_ID) ?: INVALID_APPWIDGET_ID
        val device = extras?.getParcelable<UIDevice>(Contracts.ARG_DEVICE)
        val shouldLogin = extras?.getBoolean(Contracts.ARG_WIDGET_SHOULD_LOGIN, false)
        val onJustLoggedIn = extras?.getBoolean(Contracts.ARG_WIDGET_ON_LOGGED_IN, false)
        val widgetIdsFromIntent = extras?.getIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS)

        val validWidgetTypeForUpdate =
            extras?.getSerializable(ARG_WIDGET_TYPE) == WidgetType.CURRENT_WEATHER_TILE

        /*
        * Only update widget on actions we have triggered:
        * a. Creation of widget
        * b. Login/Logout
        * c. Work Manager Update
         */
        val shouldUpdate = intent?.action == ACTION_APPWIDGET_UPDATE
            && extras?.getBoolean(ARG_IS_CUSTOM_APPWIDGET_UPDATE) ?: false

        if (!shouldUpdate) {
            return
        }

        if (appWidgetId != INVALID_APPWIDGET_ID && validWidgetTypeForUpdate) {
            /**
             * Fix when adding a widget for the first time, without this delay that widget won't be
             * able to render correctly. So the delay here is an ugly, but working fix.
             *
             * TODO: Explore this more to find the root of this bug.
             */
            widgetHelper.getWidgetIds().onRight {
                if (it.size == 1) {
                    runBlocking {
                        delay(500L)
                    }
                }
            }

            updateWidget(context, shouldLogin, device, appWidgetId)
        } else if (widgetIdsFromIntent != null && widgetIdsFromIntent.isNotEmpty()) {
            updateAllWidgets(context, shouldLogin, onJustLoggedIn, widgetIdsFromIntent)
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        CurrentWeatherWidgetWorkerUpdate.restartAllWorkers(context)
    }

    override fun onDeleted(context: Context?, appWidgetIds: IntArray?) {
        super.onDeleted(context, appWidgetIds)
        if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
            usecase.removeWidgetId(appWidgetIds[0])
        }
    }

    private fun updateWidget(
        context: Context,
        shouldLogin: Boolean?,
        device: UIDevice?,
        appWidgetId: Int
    ) {
        val widgetManager = AppWidgetManager.getInstance(context)
        if (shouldLogin == true) {
            onShouldLogin(context, widgetManager, appWidgetId)
        } else if (device == null) {
            // TODO: Do what?!
        } else {
            onDevice(context, widgetManager, appWidgetId, device)
        }
    }

    private fun updateAllWidgets(
        context: Context,
        shouldLogin: Boolean?,
        onJustLoggedIn: Boolean?,
        widgetIdsFromIntent: IntArray
    ) {
        val widgetManager = AppWidgetManager.getInstance(context)
        val widgetsToUpdate = widgetIdsFromIntent.filter {
            widgetHelper.getWidgetTypeById(widgetManager, it).apply {
                if (this == null) usecase.removeWidgetId(it)
            } == WidgetType.CURRENT_WEATHER_TILE
        }
        if (shouldLogin == true) {
            widgetsToUpdate.forEach {
                onShouldLogin(context, widgetManager, it)
            }
        } else if (onJustLoggedIn == true) {
            widgetsToUpdate.forEach {
                CurrentWeatherWidgetWorkerUpdate.initAndStart(
                    context,
                    it,
                    usecase.getWidgetDevice(it) ?: ""
                )
            }
        }
    }

    private fun onShouldLogin(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_current_weather_tile)

        views.setViewVisibility(R.id.deviceLayout, View.GONE)
        views.setViewVisibility(R.id.signInLayout, View.VISIBLE)

        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, LoginActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.root, pendingIntent)
        views.setOnClickPendingIntent(R.id.loginBtn, pendingIntent)

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun onDevice(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        device: UIDevice
    ) {
        if (device.isEmpty()) return

        val views = RemoteViews(context.packageName, R.layout.widget_current_weather_tile)

        views.setViewVisibility(R.id.signInLayout, View.GONE)
        views.setViewVisibility(R.id.deviceLayout, View.VISIBLE)

        val deviceDetailsActivity = Intent(context, DeviceDetailsActivity::class.java)
            .putExtra(Contracts.ARG_DEVICE, device)

        val pendingIntent: PendingIntent = TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(deviceDetailsActivity)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getPendingIntent(appWidgetId, FLAG_CANCEL_CURRENT or FLAG_MUTABLE)
            } else {
                getPendingIntent(appWidgetId, FLAG_CANCEL_CURRENT)
            }
        }
        views.setOnClickPendingIntent(R.id.root, pendingIntent)

        views.setTextViewText(R.id.name, device.getDefaultOrFriendlyName())
        views.setTextViewText(R.id.address, device.address)

        setStatus(context, views, device)

        if (device.currentWeather == null || device.currentWeather.isEmpty()) {
            views.setViewPadding(R.id.root, 2, 2, 2, 2)
            views.setInt(
                R.id.root,
                "setBackgroundResource",
                R.drawable.background_rounded_corners_error_stroke
            )
        } else {
            views.setInt(
                R.id.root,
                "setBackgroundResource",
                R.drawable.background_rounded_corners
            )
            views.setViewPadding(R.id.root, 0, 0, 0, 0)
            setWeatherData(context, views, device)
        }

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun setStatus(context: Context, views: RemoteViews, device: UIDevice) {
        views.setImageViewResource(
            R.id.statusIcon,
            if (device.profile == DeviceProfile.Helium) {
                R.drawable.ic_helium
            } else {
                R.drawable.ic_wifi
            }
        )
        when (device.isActive) {
            true -> {
                views.setTextViewText(
                    R.id.lastSeen,
                    device.lastWeatherStationActivity?.getFormattedTime(context)
                )
                views.setInt(
                    R.id.statusCard,
                    "setBackgroundResource",
                    R.drawable.background_rounded_corners_success
                )
            }

            false -> {
                views.setTextViewText(
                    R.id.lastSeen,
                    device.lastWeatherStationActivity?.getRelativeFormattedTime(
                        context.getString(R.string.just_now)
                    )
                )
                views.setInt(
                    R.id.statusCard,
                    "setBackgroundResource",
                    R.drawable.background_rounded_corners_error
                )
            }

            else -> {}
        }
    }

    private fun setWeatherData(context: Context, views: RemoteViews, device: UIDevice) {
        Weather.getWeatherStaticIcon(device.currentWeather?.icon)?.let {
            views.setImageViewResource(R.id.weatherIcon, it)
        }

        val temperature = Weather.getFormattedTemperature(
            device.currentWeather?.temperature, 1, includeUnit = false
        )
        val temperatureUnit = Weather.getPreferredUnit(
            context.getString(CacheService.KEY_TEMPERATURE),
            context.getString(R.string.temperature_celsius)
        )
        views.setTextViewText(R.id.temperature, temperature)
        views.setTextViewText(R.id.temperatureUnit, temperatureUnit)

        val feelsLike = Weather.getFormattedTemperature(
            device.currentWeather?.feelsLike, 1, includeUnit = false
        )
        val feelsLikeUnit = Weather.getPreferredUnit(
            context.getString(CacheService.KEY_TEMPERATURE),
            context.getString(R.string.temperature_celsius)
        )
        views.setTextViewText(R.id.feelsLike, feelsLike)
        views.setTextViewText(R.id.feelsLikeUnit, feelsLikeUnit)
    }
}
