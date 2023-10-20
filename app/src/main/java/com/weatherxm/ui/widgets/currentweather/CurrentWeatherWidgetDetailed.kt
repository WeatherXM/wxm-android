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
import com.weatherxm.ui.common.Contracts.ARG_DEVICE
import com.weatherxm.ui.common.Contracts.ARG_IS_CUSTOM_APPWIDGET_UPDATE
import com.weatherxm.ui.common.Contracts.ARG_WIDGET_ON_LOGGED_IN
import com.weatherxm.ui.common.Contracts.ARG_WIDGET_SHOULD_LOGIN
import com.weatherxm.ui.common.Contracts.ARG_WIDGET_TYPE
import com.weatherxm.ui.common.DeviceRelation
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.devicedetails.DeviceDetailsActivity
import com.weatherxm.ui.login.LoginActivity
import com.weatherxm.ui.widgets.WidgetType
import com.weatherxm.ui.widgets.selectstation.SelectStationActivity
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
 * Implementation of Current Weather Widget functionality.
 */
class CurrentWeatherWidgetDetailed : AppWidgetProvider(), KoinComponent {
    private val usecase: WidgetCurrentWeatherUseCase by inject()
    private val widgetHelper: WidgetHelper by inject()

    /**
     * OnReceive is the receiver for catching specific intents
     */
    @Suppress("MagicNumber")
    override fun onReceive(context: Context, intent: Intent?) {
        super.onReceive(context, intent)

        // These variables are useful for identifying what type of update to do
        val extras = intent?.extras
        val appWidgetId =
            extras?.getInt(EXTRA_APPWIDGET_ID, INVALID_APPWIDGET_ID) ?: INVALID_APPWIDGET_ID
        val device = extras?.getParcelable<UIDevice>(ARG_DEVICE)
        val shouldLogin = extras?.getBoolean(ARG_WIDGET_SHOULD_LOGIN, false)
        val shouldSelectStation =
            extras?.getBoolean(Contracts.ARG_WIDGET_SHOULD_SELECT_STATION, false)
        val onJustLoggedIn = extras?.getBoolean(ARG_WIDGET_ON_LOGGED_IN, false)
        val widgetIdsFromIntent = extras?.getIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS)

        val validWidgetTypeForUpdate =
            extras?.getSerializable(ARG_WIDGET_TYPE) == WidgetType.CURRENT_WEATHER_DETAILED

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

            updateWidget(context, shouldLogin, shouldSelectStation, device, appWidgetId)
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
        shouldSelectStation: Boolean?,
        device: UIDevice?,
        appWidgetId: Int
    ) {
        val widgetManager = AppWidgetManager.getInstance(context)

        if (shouldLogin == true) {
            onShouldLogin(context, widgetManager, appWidgetId)
        } else if (shouldSelectStation == true) {
            onShouldSelectStation(context, widgetManager, appWidgetId)
        } else if (device == null) {
            onError(context, widgetManager, appWidgetId)
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
            } == WidgetType.CURRENT_WEATHER_DETAILED
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

    private fun onShouldSelectStation(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_current_weather_detailed)

        views.setViewVisibility(R.id.deviceLayout, View.GONE)
        views.setViewVisibility(R.id.errorLayout, View.GONE)
        views.setTextViewText(R.id.actionTitle, context.getString(R.string.action_select_station))
        views.setTextViewText(
            R.id.actionDesc,
            context.getString(R.string.please_select_station_desc)
        )
        views.setTextViewText(R.id.actionBtn, context.getString(R.string.action_select_station))
        views.setViewVisibility(R.id.actionLayout, View.VISIBLE)

        val intent = Intent(context, SelectStationActivity::class.java)
        intent.putExtra(EXTRA_APPWIDGET_ID, appWidgetId)
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            appWidgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.root, pendingIntent)
        views.setOnClickPendingIntent(R.id.actionBtn, pendingIntent)

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun onShouldLogin(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_current_weather_detailed)

        views.setViewVisibility(R.id.deviceLayout, View.GONE)
        views.setViewVisibility(R.id.errorLayout, View.GONE)
        views.setTextViewText(R.id.actionTitle, context.getString(R.string.please_sign_in))
        views.setTextViewText(R.id.actionDesc, context.getString(R.string.please_sign_in_desc))
        views.setTextViewText(R.id.actionBtn, context.getString(R.string.action_sign_in))
        views.setViewVisibility(R.id.actionLayout, View.VISIBLE)

        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, LoginActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.root, pendingIntent)
        views.setOnClickPendingIntent(R.id.actionBtn, pendingIntent)

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun onError(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_current_weather_detailed)

        views.setViewVisibility(R.id.deviceLayout, View.GONE)
        views.setViewVisibility(R.id.actionLayout, View.GONE)
        views.setViewVisibility(R.id.errorLayout, View.VISIBLE)

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun onDevice(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        device: UIDevice
    ) {
        if (device.isEmpty() || appWidgetId == INVALID_APPWIDGET_ID) return

        val views = RemoteViews(context.packageName, R.layout.widget_current_weather_detailed)
        views.setViewVisibility(R.id.errorLayout, View.GONE)
        views.setViewVisibility(R.id.actionLayout, View.GONE)
        views.setViewVisibility(R.id.deviceLayout, View.VISIBLE)

        val deviceDetailsActivity = Intent(context, DeviceDetailsActivity::class.java)
            .putExtra(ARG_DEVICE, device)

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

        @Suppress("UseCheckOrError")
        views.setImageViewResource(
            R.id.stationHomeFollowIcon,
            when (device.relation) {
                DeviceRelation.OWNED -> R.drawable.ic_home
                DeviceRelation.FOLLOWED -> R.drawable.ic_favorite
                DeviceRelation.UNFOLLOWED -> R.drawable.ic_favorite_outline
                null -> throw IllegalStateException("Oops! No device relation here.")
            }
        )

        setStatus(context, views, device)

        if (device.currentWeather == null || device.currentWeather.isEmpty()) {
            views.setViewVisibility(R.id.weatherDataLayout, View.GONE)
            views.setViewVisibility(R.id.noDataLayout, View.VISIBLE)
            views.setViewPadding(R.id.root, 2, 2, 2, 2)
            views.setInt(
                R.id.root,
                "setBackgroundResource",
                R.drawable.background_rounded_surface_error_stroke
            )
        } else {
            views.setViewVisibility(R.id.noDataLayout, View.GONE)
            views.setInt(R.id.root, "setBackgroundResource", R.drawable.background_rounded_surface)
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
                views.setTextColor(
                    R.id.lastSeen,
                    context.getColor(R.color.status_chip_content_online)
                )
                views.setInt(
                    R.id.statusIcon,
                    "setColorFilter",
                    context.getColor(R.color.status_chip_content_online)
                )
                views.setInt(
                    R.id.statusContainer,
                    "setBackgroundResource",
                    R.drawable.background_rounded_success
                )
            }
            false -> {
                views.setTextViewText(
                    R.id.lastSeen,
                    device.lastWeatherStationActivity?.getRelativeFormattedTime()
                )
                views.setTextColor(
                    R.id.lastSeen,
                    context.getColor(R.color.status_chip_content_offline)
                )
                views.setInt(
                    R.id.statusIcon,
                    "setColorFilter",
                    context.getColor(R.color.status_chip_content_offline)
                )
                views.setInt(
                    R.id.statusContainer,
                    "setBackgroundResource",
                    R.drawable.background_rounded_error
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

        val humidity = Weather.getFormattedHumidity(device.currentWeather?.humidity, false)
        views.setTextViewText(R.id.humidityValue, humidity)
        views.setTextViewText(R.id.humidityUnit, "%")

        val windSpeedValue = Weather.getFormattedWind(
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
        views.setTextViewText(R.id.windValue, windSpeedValue)
        views.setTextViewText(R.id.windUnit, "$windUnit $windDirectionUnit")

        val rainRateValue = Weather.getFormattedPrecipitation(
            device.currentWeather?.precipitation, includeUnit = false
        )
        views.setTextViewText(R.id.rainRateValue, rainRateValue)
        views.setTextViewText(R.id.rainRateUnit, Weather.getPrecipitationPreferredUnit())

        val windGustValue = Weather.getFormattedWind(
            device.currentWeather?.windGust,
            device.currentWeather?.windDirection,
            includeUnits = false
        )
        views.setTextViewText(R.id.windGustValue, windGustValue)
        views.setTextViewText(R.id.windGustUnit, "$windUnit $windDirectionUnit")

        val dailyPrecipValue = Weather.getFormattedPrecipitation(
            device.currentWeather?.precipAccumulated, isRainRate = false, includeUnit = false
        )
        views.setTextViewText(R.id.dailyPrecipValue, dailyPrecipValue)
        views.setTextViewText(R.id.dailyPrecipUnit, Weather.getPrecipitationPreferredUnit(false))

        val pressureValue = Weather.getFormattedPressure(
            device.currentWeather?.pressure, includeUnit = false
        )
        val pressureUnit = Weather.getPreferredUnit(
            context.getString(CacheService.KEY_PRESSURE),
            context.getString(R.string.pressure_hpa)
        )
        views.setTextViewText(R.id.pressureValue, pressureValue)
        views.setTextViewText(R.id.pressureUnit, pressureUnit)

        val dewPointValue = Weather.getFormattedTemperature(
            device.currentWeather?.dewPoint, decimals = 1, includeUnit = false
        )
        views.setTextViewText(R.id.dewValue, dewPointValue)
        views.setTextViewText(R.id.dewUnit, temperatureUnit)

        val radiationValue = Weather.getFormattedSolarRadiation(
            device.currentWeather?.solarIrradiance, includeUnit = false
        )
        views.setTextViewText(R.id.radiationValue, radiationValue)
        views.setTextViewText(R.id.radiationUnit, context.getString(R.string.solar_radiation_unit))

        views.setTextViewText(R.id.uvValue, Weather.getFormattedUV(device.currentWeather?.uvIndex))
    }
}
