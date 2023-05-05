package com.weatherxm.ui.widgets.currentweather

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_CANCEL_CURRENT
import android.app.PendingIntent.FLAG_MUTABLE
import android.app.TaskStackBuilder
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
import android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID
import android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS
import android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import com.weatherxm.R
import com.weatherxm.data.Device
import com.weatherxm.data.DeviceProfile
import com.weatherxm.data.services.CacheService
import com.weatherxm.data.services.CacheService.Companion.WIDGET_CURRENT_WEATHER_TILE_PREFIX
import com.weatherxm.ui.common.Contracts
import com.weatherxm.ui.common.Contracts.ARG_IS_CUSTOM_APPWIDGET_UPDATE
import com.weatherxm.ui.common.Contracts.ARG_WIDGET_TYPE
import com.weatherxm.ui.login.LoginActivity
import com.weatherxm.ui.userdevice.UserDeviceActivity
import com.weatherxm.ui.widgets.WidgetType
import com.weatherxm.usecases.WidgetCurrentWeatherUseCase
import com.weatherxm.util.DateTimeHelper.getFormattedTime
import com.weatherxm.util.Weather
import com.weatherxm.util.WidgetHelper
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

/**
 * Implementation of Current Weather Widget Tile functionality.
 */
class CurrentWeatherWidgetTile : AppWidgetProvider(), KoinComponent {
    private val usecase: WidgetCurrentWeatherUseCase by inject()
    private val widgetHelper: WidgetHelper by inject()

    @Suppress("MagicNumber")
    override fun onReceive(context: Context, intent: Intent?) {
        super.onReceive(context, intent)

        // These 2 variables are useful for when we first add the widget
        val appWidgetId =
            intent?.extras?.getInt(EXTRA_APPWIDGET_ID, INVALID_APPWIDGET_ID) ?: INVALID_APPWIDGET_ID
        val isValidWidgetType =
            intent?.extras?.getSerializable(ARG_WIDGET_TYPE) == WidgetType.CURRENT_WEATHER_TILE

        // Only update widget on actions we have triggered
        val shouldUpdate = intent?.action == ACTION_APPWIDGET_UPDATE && intent.extras?.getBoolean(
            ARG_IS_CUSTOM_APPWIDGET_UPDATE
        ) ?: false

        // This should not be empty on each work manager trigger or on logged in trigger
        val idsOfWidgetsToUpdate = widgetHelper.getWidgetsOfType(
            intent?.extras?.getIntArray(EXTRA_APPWIDGET_IDS),
            WIDGET_CURRENT_WEATHER_TILE_PREFIX
        )

        if (appWidgetId != INVALID_APPWIDGET_ID && shouldUpdate && isValidWidgetType) {
            Timber.d("Widget added.")
            /**
             * Fix when adding a widget for the first time, without this delay that widget won't be
             * able to render correctly. So the delay here is an ugly, but working fix.
             *
             * TODO: Explore this more to find the root of this bug.
             */
            widgetHelper.getWidgetIds().onRight {
                if (it.size == 1) {
                    runBlocking {
                        delay(1000L)
                    }
                }
            }

            widgetHelper.setWidgetOfType(appWidgetId, WIDGET_CURRENT_WEATHER_TILE_PREFIX)
            GlobalScope.launch {
                usecase.isLoggedIn().onRight {
                    if (it) {
                        getDeviceAndUpdate(context, appWidgetId)
                    } else {
                        onShouldSignIn(context, AppWidgetManager.getInstance(context), appWidgetId)
                    }
                }.onLeft {
                    onShouldSignIn(context, AppWidgetManager.getInstance(context), appWidgetId)
                }
            }
        } else if (idsOfWidgetsToUpdate.isNotEmpty() && shouldUpdate) {
            Timber.d("Widget Work Manager or Logged In Triggered.")
            GlobalScope.launch {
                usecase.isLoggedIn().onRight {
                    if (it) {
                        getDevicesAndUpdate(context, idsOfWidgetsToUpdate)
                    } else {
                        idsOfWidgetsToUpdate.forEach { widgetId ->
                            onShouldSignIn(context, AppWidgetManager.getInstance(context), widgetId)
                        }
                    }
                }.onLeft {
                    idsOfWidgetsToUpdate.forEach { widgetId ->
                        onShouldSignIn(context, AppWidgetManager.getInstance(context), widgetId)
                    }
                }
            }
        }
    }

    override fun onDeleted(context: Context?, appWidgetIds: IntArray?) {
        super.onDeleted(context, appWidgetIds)
        if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
            usecase.removeWidgetId(appWidgetIds[0])
            widgetHelper.setWidgetOfType(appWidgetIds[0], WIDGET_CURRENT_WEATHER_TILE_PREFIX, false)
        }
    }

    private suspend fun getDevicesAndUpdate(context: Context, appWidgetIds: List<Int>) {
        val deviceIds = mutableListOf<String>()
        val deviceIdsWithWidgetIds = mutableMapOf<String, MutableList<Int>>()
        appWidgetIds.forEach {
            usecase.getDeviceOfWidget(it).onRight { deviceId ->
                deviceIds.add(deviceId)
                val widgetIds = deviceIdsWithWidgetIds.getOrDefault(deviceId, mutableListOf())
                widgetIds.add(it)
                deviceIdsWithWidgetIds[deviceId] = widgetIds
            }.onLeft { failure ->
                Timber.w("Fetching device ID of widget failed: $failure")
                // TODO: Do what?
            }
        }

        usecase.getUserDevices(deviceIds).onRight { devices ->
            devices.forEach {
                deviceIdsWithWidgetIds[it.id]?.forEach { widgetId ->
                    updateWidget(
                        context,
                        AppWidgetManager.getInstance(context),
                        widgetId,
                        it
                    )
                }
            }
        }.onLeft {
            Timber.w("Fetching user devices for widgets failed: $it")
            // TODO: Do what?
        }
    }

    private suspend fun getDeviceAndUpdate(context: Context, appWidgetId: Int) {
        usecase.getDeviceOfWidget(appWidgetId).onRight { deviceId ->
            usecase.getUserDevice(deviceId).onRight { device ->
                updateWidget(
                    context,
                    AppWidgetManager.getInstance(context),
                    appWidgetId,
                    device
                )
            }.onLeft {
                Timber.w("Fetching user device for widget failed: $it")
                // TODO: Do what?
            }
        }.onLeft {
            Timber.w("Fetching device ID of widget failed: $it")
            // TODO: Do what?
        }
    }

    private fun onShouldSignIn(
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

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        device: Device
    ) {
        if (device.isEmpty()) return

        val views = RemoteViews(context.packageName, R.layout.widget_current_weather_tile)

        views.setViewVisibility(R.id.signInLayout, View.GONE)
        views.setViewVisibility(R.id.deviceLayout, View.VISIBLE)

        val userDeviceActivity = Intent(context, UserDeviceActivity::class.java)
            .putExtra(Contracts.ARG_DEVICE, device)

        val pendingIntent: PendingIntent = TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(userDeviceActivity)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getPendingIntent(appWidgetId, FLAG_CANCEL_CURRENT or FLAG_MUTABLE)
            } else {
                getPendingIntent(appWidgetId, FLAG_CANCEL_CURRENT)
            }
        }
        views.setOnClickPendingIntent(R.id.root, pendingIntent)

        views.setTextViewText(R.id.name, device.getNameOrLabel())
        views.setTextViewText(R.id.address, device.address)
        views.setTextViewText(
            R.id.lastSeen,
            device.attributes?.lastWeatherStationActivity?.getFormattedTime(context)
        )

        setStatusIcon(context, views, device)

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

    private fun setStatusIcon(context: Context, views: RemoteViews, device: Device) {
        views.setImageViewResource(
            R.id.status_icon,
            if (device.profile == DeviceProfile.Helium) {
                R.drawable.ic_helium
            } else {
                R.drawable.ic_wifi
            }
        )
        when (device.attributes?.isActive) {
            true -> {
                views.setInt(
                    R.id.status,
                    "setBackgroundResource",
                    R.drawable.background_rounded_corners_success
                )
                views.setInt(
                    R.id.status_icon,
                    "setColorFilter",
                    context.getColor(R.color.success)
                )
            }

            false -> {
                views.setInt(
                    R.id.status,
                    "setBackgroundResource",
                    R.drawable.background_rounded_corners_error
                )
                views.setInt(
                    R.id.status_icon,
                    "setColorFilter",
                    context.getColor(R.color.error)
                )
            }

            else -> {}
        }
    }

    private fun setWeatherData(context: Context, views: RemoteViews, device: Device) {
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