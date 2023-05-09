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
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.weatherxm.R
import com.weatherxm.data.Device
import com.weatherxm.data.DeviceProfile
import com.weatherxm.data.services.CacheService
import com.weatherxm.data.services.CacheService.Companion.WIDGET_CURRENT_WEATHER_PREFIX
import com.weatherxm.ui.common.Contracts
import com.weatherxm.ui.common.Contracts.ARG_IS_CUSTOM_APPWIDGET_UPDATE
import com.weatherxm.ui.common.Contracts.ARG_WIDGET_TYPE
import com.weatherxm.ui.login.LoginActivity
import com.weatherxm.ui.userdevice.UserDeviceActivity
import com.weatherxm.ui.widgets.WidgetType
import com.weatherxm.usecases.WidgetCurrentWeatherUseCase
import com.weatherxm.util.DateTimeHelper.getFormattedTime
import com.weatherxm.util.DateTimeHelper.getRelativeFormattedTime
import com.weatherxm.util.Weather
import com.weatherxm.util.WidgetHelper
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

/**
 * Implementation of Current Weather Widget functionality.
 */
class CurrentWeatherWidget : AppWidgetProvider(), KoinComponent {
    private val usecase: WidgetCurrentWeatherUseCase by inject()
    private val widgetHelper: WidgetHelper by inject()
    private val firebaseCrashlytics: FirebaseCrashlytics by inject()

    /**
     * OnReceive is the receiver for catching specific intents
     */
    @OptIn(DelicateCoroutinesApi::class)
    @Suppress("MagicNumber")
    override fun onReceive(context: Context, intent: Intent?) {
        super.onReceive(context, intent)

        // These 2 variables are useful for when we first add the widget
        val appWidgetId =
            intent?.extras?.getInt(EXTRA_APPWIDGET_ID, INVALID_APPWIDGET_ID) ?: INVALID_APPWIDGET_ID
        val isValidWidgetType =
            intent?.extras?.getSerializable(ARG_WIDGET_TYPE) == WidgetType.CURRENT_WEATHER

        // Only update widget on actions we have triggered
        val shouldUpdate = intent?.action == ACTION_APPWIDGET_UPDATE && intent.extras?.getBoolean(
            ARG_IS_CUSTOM_APPWIDGET_UPDATE
        ) ?: false

        /*
         * This should not be empty on each work manager trigger or on logged in/out trigger.
         *
         * The "getWidgetsOfType" is returning all widget ids of this type (CurrentWeatherWidget.kt)
         * in order to update them afterwards.
         */
        val idsOfWidgetsToUpdate = widgetHelper.getWidgetsOfType(
            intent?.extras?.getIntArray(EXTRA_APPWIDGET_IDS),
            WIDGET_CURRENT_WEATHER_PREFIX
        )

        /**
         * The first "if" should run when we first add the widget and we have a specific widget id.
         *
         * The "else if" should run on each ARG_IS_CUSTOM_APPWIDGET_UPDATE like on logged in/out
         * or when the WorkManager triggers by broadcasting an explicit intent containing
         * the respective data.
         */
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

            /**
             * We need to save the widgetId with its type like CurrentWeatherWidget.kt,
             * or the tile widget etc by saving it in the cache,
             * so we do it here when we have just added the widget.
             */
            widgetHelper.setWidgetOfType(appWidgetId, WIDGET_CURRENT_WEATHER_PREFIX)

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
            widgetHelper.setWidgetOfType(appWidgetIds[0], WIDGET_CURRENT_WEATHER_PREFIX, false)
        }
    }

    private suspend fun getDevicesAndUpdate(context: Context, appWidgetIds: List<Int>) {
        val deviceIds = mutableListOf<String>()
        val deviceIdsWithWidgetIds = mutableMapOf<String, MutableList<Int>>()
        /**
         * For each appWidgetId, we search in cache to get the deviceId associated with this widget,
         * and
         *
         * 1. We add that deviceId to the `deviceIds` list in order to make 1 API call for all
         * 2. We populate the `deviceIdsWithWidgetIds`. For each deviceId we find
         * (specified by the `String` key in the map above) we save all the widgetIds
         */
        appWidgetIds.forEach {
            usecase.getDeviceOfWidget(it).onRight { deviceId ->
                deviceIds.add(deviceId)
                val widgetIds = deviceIdsWithWidgetIds.getOrDefault(deviceId, mutableListOf())
                widgetIds.add(it)
                deviceIdsWithWidgetIds[deviceId] = widgetIds
            }.onLeft { failure ->
                Timber.w("Fetching device ID of widget failed: $failure")
                // TODO: Remove these custom logs when we find the root of the onError being shown
                firebaseCrashlytics.recordException(
                    Exception("Fetching device ID of widget failed: $it")
                )
                onError(context, AppWidgetManager.getInstance(context), it)
            }
        }

        usecase.getUserDevices(deviceIds).onRight { devices ->
            /**
             * For each device we get, update the respective widget
             * by using the map we populated before.
             */
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
            firebaseCrashlytics.recordException(
                Exception("Fetching user devices for widgets failed: $it")
            )
            appWidgetIds.forEach { appWidgetId ->
                onError(context, AppWidgetManager.getInstance(context), appWidgetId)
            }
        }
    }

    /**
     * This runs only when we first add the widget when we want to update it for the first time
     * (i.e. without the "batch updating" we do on `getDevicesAndUpdate`)
     */
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
                firebaseCrashlytics.recordException(
                    Exception("Fetching user device for widget failed: $it")
                )
                onError(context, AppWidgetManager.getInstance(context), appWidgetId)
            }
        }.onLeft {
            Timber.w("Fetching device ID of widget failed: $it")
            firebaseCrashlytics.recordException(
                Exception("Fetching device ID of widget failed: $it")
            )
            onError(context, AppWidgetManager.getInstance(context), appWidgetId)
        }
    }

    private fun onShouldSignIn(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_current_weather)

        views.setViewVisibility(R.id.deviceLayout, View.GONE)
        views.setViewVisibility(R.id.errorLayout, View.GONE)
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

    private fun onError(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_current_weather)

        views.setViewVisibility(R.id.deviceLayout, View.GONE)
        views.setViewVisibility(R.id.signInLayout, View.GONE)
        views.setViewVisibility(R.id.errorLayout, View.VISIBLE)

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        device: Device
    ) {
        if (device.isEmpty() || appWidgetId == INVALID_APPWIDGET_ID) return

        val views = RemoteViews(context.packageName, R.layout.widget_current_weather)
        views.setViewVisibility(R.id.errorLayout, View.GONE)
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

        setStatus(context, views, device)

        if (device.currentWeather == null || device.currentWeather.isEmpty()) {
            views.setViewVisibility(R.id.weatherDataLayout, View.GONE)
            views.setViewVisibility(R.id.noDataLayout, View.VISIBLE)
            views.setViewPadding(R.id.root, 2, 2, 2, 2)
            views.setInt(
                R.id.root,
                "setBackgroundResource",
                R.drawable.background_rounded_corners_error_stroke
            )
        } else {
            views.setInt(R.id.root, "setBackgroundResource", R.drawable.background_rounded_corners)
            views.setViewPadding(R.id.root, 0, 0, 0, 0)
            setWeatherData(context, views, device)
        }

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun setStatus(context: Context, views: RemoteViews, device: Device) {
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
                views.setTextViewText(
                    R.id.lastSeen,
                    device.attributes.lastWeatherStationActivity?.getFormattedTime(context)
                )
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
                views.setTextViewText(
                    R.id.lastSeen,
                    device.attributes.lastWeatherStationActivity?.getRelativeFormattedTime(
                        context.getString(R.string.just_now)
                    )
                )
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

        val humidity = Weather.getFormattedHumidity(device.currentWeather?.humidity, false)
        views.setTextViewText(R.id.humidityValue, humidity)
        views.setTextViewText(R.id.humidityUnit, "%")

        val windValue = Weather.getFormattedWind(
            device.currentWeather?.windSpeed,
            device.currentWeather?.windDirection,
            includeUnits = false
        )
        val windUnit = Weather.getPreferredUnit(
            context.getString(CacheService.KEY_WIND),
            context.getString(R.string.wind_speed_ms)
        )
        val windDirectionUnit = device.currentWeather?.windDirection?.let {
            Weather.getFormattedWindDirection(it)
        } ?: ""
        views.setTextViewText(R.id.windValue, windValue)
        views.setTextViewText(R.id.windUnit, "$windUnit $windDirectionUnit")

        val precipitationValue = Weather.getFormattedPrecipitation(
            device.currentWeather?.precipitation, includeUnit = false
        )
        views.setTextViewText(R.id.precipitationValue, precipitationValue)
        views.setTextViewText(R.id.precipitationUnit, Weather.getPrecipitationPreferredUnit())
    }
}
