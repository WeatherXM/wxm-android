package com.weatherxm.ui.widgets

import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import com.weatherxm.R
import com.weatherxm.data.DeviceProfile
import com.weatherxm.data.services.CacheService.Companion.KEY_PRESSURE
import com.weatherxm.data.services.CacheService.Companion.KEY_TEMPERATURE
import com.weatherxm.data.services.CacheService.Companion.KEY_WIND
import com.weatherxm.ui.common.Contracts
import com.weatherxm.ui.common.DeviceRelation
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.devicedetails.DeviceDetailsActivity
import com.weatherxm.ui.login.LoginActivity
import com.weatherxm.ui.widgets.selectstation.SelectStationActivity
import com.weatherxm.util.DateTimeHelper.getFormattedDate
import com.weatherxm.util.DateTimeHelper.getFormattedTime
import com.weatherxm.util.DateTimeHelper.getRelativeFormattedTime
import com.weatherxm.util.Weather
import com.weatherxm.util.isToday

fun RemoteViews.onShouldSelectStation(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    setViewVisibility(R.id.deviceLayout, View.GONE)
    setViewVisibility(R.id.errorLayout, View.GONE)
    setTextViewText(R.id.actionTitle, context.getString(R.string.action_select_station))
    setTextViewText(
        R.id.actionDesc,
        context.getString(R.string.please_select_station_desc)
    )
    setTextViewText(R.id.actionBtn, context.getString(R.string.action_select_station))
    setViewVisibility(R.id.actionLayout, View.VISIBLE)

    val intent = Intent(context, SelectStationActivity::class.java)
    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    val pendingIntent: PendingIntent = PendingIntent.getActivity(
        context,
        appWidgetId,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    setOnClickPendingIntent(R.id.root, pendingIntent)
    setOnClickPendingIntent(R.id.actionBtn, pendingIntent)

    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, this)
}

fun RemoteViews.onShouldLogin(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    setViewVisibility(R.id.deviceLayout, View.GONE)
    setViewVisibility(R.id.errorLayout, View.GONE)
    setTextViewText(R.id.actionTitle, context.getString(R.string.please_sign_in))
    setTextViewText(R.id.actionDesc, context.getString(R.string.please_sign_in_desc))
    setTextViewText(R.id.actionBtn, context.getString(R.string.action_sign_in))
    setViewVisibility(R.id.actionLayout, View.VISIBLE)

    val pendingIntent: PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, LoginActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    setOnClickPendingIntent(R.id.root, pendingIntent)
    setOnClickPendingIntent(R.id.actionBtn, pendingIntent)

    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, this)
}

fun RemoteViews.onError(appWidgetManager: AppWidgetManager, appWidgetId: Int) {
    setViewVisibility(R.id.deviceLayout, View.GONE)
    setViewVisibility(R.id.actionLayout, View.GONE)
    setViewVisibility(R.id.errorLayout, View.VISIBLE)

    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, this)
}

fun RemoteViews.onDevice(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
    device: UIDevice,
    widgetType: WidgetType
) {
    if (device.isEmpty() || appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return

    setViewVisibility(R.id.errorLayout, View.GONE)
    setViewVisibility(R.id.actionLayout, View.GONE)
    setViewVisibility(R.id.deviceLayout, View.VISIBLE)

    val deviceDetailsActivity = Intent(context, DeviceDetailsActivity::class.java)
        .putExtra(Contracts.ARG_DEVICE, device)

    val pendingIntent: PendingIntent = TaskStackBuilder.create(context).run {
        addNextIntentWithParentStack(deviceDetailsActivity)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getPendingIntent(
                appWidgetId,
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_MUTABLE
            )
        } else {
            getPendingIntent(appWidgetId, PendingIntent.FLAG_CANCEL_CURRENT)
        }
    }
    setOnClickPendingIntent(R.id.root, pendingIntent)

    setTextViewText(R.id.name, device.getDefaultOrFriendlyName())
    setTextViewText(R.id.address, device.address)

    @Suppress("UseCheckOrError")
    setImageViewResource(
        R.id.stationHomeFollowIcon,
        when (device.relation) {
            DeviceRelation.OWNED -> R.drawable.ic_home
            DeviceRelation.FOLLOWED -> R.drawable.ic_favorite
            DeviceRelation.UNFOLLOWED -> R.drawable.ic_favorite_outline
            null -> throw IllegalStateException("Oops! No device relation here.")
        }
    )

    this.setStatus(context, device, widgetType)

    if (device.currentWeather == null || device.currentWeather.isEmpty()) {
        setViewVisibility(R.id.weatherDataLayout, View.GONE)
        setViewVisibility(R.id.noDataLayout, View.VISIBLE)
        setViewPadding(R.id.root, 2, 2, 2, 2)
        val backgroundResId = if (widgetType == WidgetType.CURRENT_WEATHER_DETAILED) {
            R.drawable.background_rounded_surface_error_stroke
        } else {
            R.drawable.background_rounded_error_stroke
        }
        setInt(R.id.root, "setBackgroundResource", backgroundResId)
    } else {
        setViewVisibility(R.id.noDataLayout, View.GONE)
        setInt(R.id.root, "setBackgroundResource", R.drawable.background_rounded_surface)
        setViewPadding(R.id.root, 0, 0, 0, 0)
        this.setWeatherData(context, device, widgetType)
    }

    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, this)
}

fun RemoteViews.setStatus(
    context: Context,
    device: UIDevice,
    widgetType: WidgetType
) {
    setImageViewResource(
        R.id.statusIcon,
        if (device.profile == DeviceProfile.Helium) {
            R.drawable.ic_helium
        } else {
            R.drawable.ic_wifi
        }
    )

    val lastSeen = if (widgetType == WidgetType.CURRENT_WEATHER_TILE) {
        if (device.lastWeatherStationActivity?.isToday() == true) {
            device.lastWeatherStationActivity.getFormattedTime(context)
        } else {
            device.lastWeatherStationActivity?.getFormattedDate()
        }
    } else {
        device.lastWeatherStationActivity?.getRelativeFormattedTime()
    }

    when (device.isActive) {
        true -> {
            setTextViewText(
                R.id.lastSeen,
                device.lastWeatherStationActivity?.getFormattedTime(context)
            )
            setTextColor(
                R.id.lastSeen,
                context.getColor(R.color.status_chip_content_online)
            )
            setInt(
                R.id.statusIcon,
                "setColorFilter",
                context.getColor(R.color.status_chip_content_online)
            )
            setInt(
                R.id.statusContainer,
                "setBackgroundResource",
                R.drawable.background_rounded_success
            )
        }
        false -> {
            setTextViewText(R.id.lastSeen, lastSeen)
            setTextColor(
                R.id.lastSeen,
                context.getColor(R.color.status_chip_content_offline)
            )
            setInt(
                R.id.statusIcon,
                "setColorFilter",
                context.getColor(R.color.status_chip_content_offline)
            )
            setInt(
                R.id.statusContainer,
                "setBackgroundResource",
                R.drawable.background_rounded_error
            )
        }
        else -> {}
    }
}

fun RemoteViews.setWeatherData(
    context: Context,
    device: UIDevice,
    widgetType: WidgetType
) {
    Weather.getWeatherStaticIcon(device.currentWeather?.icon)?.let {
        setImageViewResource(R.id.weatherIcon, it)
    }

    val temperature = Weather.getFormattedTemperature(
        device.currentWeather?.temperature, 1, includeUnit = false
    )
    val temperatureUnit = Weather.getPreferredUnit(
        context.getString(KEY_TEMPERATURE), context.getString(R.string.temperature_celsius)
    )
    setTextViewText(R.id.temperature, temperature)
    setTextViewText(R.id.temperatureUnit, temperatureUnit)

    val feelsLike = Weather.getFormattedTemperature(
        device.currentWeather?.feelsLike, 1, includeUnit = false
    )
    val feelsLikeUnit = Weather.getPreferredUnit(
        context.getString(KEY_TEMPERATURE), context.getString(R.string.temperature_celsius)
    )
    setTextViewText(R.id.feelsLike, feelsLike)
    setTextViewText(R.id.feelsLikeUnit, feelsLikeUnit)

    /**
     * Data shown on all widgets except the tile
     */
    val windUnit = Weather.getPreferredUnit(
        context.getString(KEY_WIND), context.getString(R.string.wind_speed_ms)
    )
    val windDirectionUnit = device.currentWeather?.windDirection?.let {
        Weather.getFormattedWindDirection(it)
    } ?: ""
    if (widgetType != WidgetType.CURRENT_WEATHER_TILE) {
        val humidity = Weather.getFormattedHumidity(device.currentWeather?.humidity, false)
        setTextViewText(R.id.humidityValue, humidity)
        setTextViewText(R.id.humidityUnit, "%")

        val windValue = Weather.getFormattedWind(
            device.currentWeather?.windSpeed,
            device.currentWeather?.windDirection,
            includeUnits = false
        )
        setTextViewText(R.id.windValue, windValue)
        setTextViewText(R.id.windUnit, "$windUnit $windDirectionUnit")

        val rainRateValue = Weather.getFormattedPrecipitation(
            device.currentWeather?.precipitation, includeUnit = false
        )
        setTextViewText(R.id.rainRateValue, rainRateValue)
        setTextViewText(R.id.rainRateUnit, Weather.getPrecipitationPreferredUnit())
    }

    /**
     * Data shown only on detailed widget
     */
    if (widgetType == WidgetType.CURRENT_WEATHER_DETAILED) {
        val windGustValue = Weather.getFormattedWind(
            device.currentWeather?.windGust,
            device.currentWeather?.windDirection,
            includeUnits = false
        )
        setTextViewText(R.id.windGustValue, windGustValue)
        setTextViewText(R.id.windGustUnit, "$windUnit $windDirectionUnit")

        val dailyPrecipValue = Weather.getFormattedPrecipitation(
            device.currentWeather?.precipAccumulated, isRainRate = false, includeUnit = false
        )
        setTextViewText(R.id.dailyPrecipValue, dailyPrecipValue)
        setTextViewText(R.id.dailyPrecipUnit, Weather.getPrecipitationPreferredUnit(false))

        val pressureValue = Weather.getFormattedPressure(
            device.currentWeather?.pressure, includeUnit = false
        )
        val pressureUnit = Weather.getPreferredUnit(
            context.getString(KEY_PRESSURE), context.getString(R.string.pressure_hpa)
        )
        setTextViewText(R.id.pressureValue, pressureValue)
        setTextViewText(R.id.pressureUnit, pressureUnit)

        val dewPointValue = Weather.getFormattedTemperature(
            device.currentWeather?.dewPoint, decimals = 1, includeUnit = false
        )
        setTextViewText(R.id.dewValue, dewPointValue)
        setTextViewText(R.id.dewUnit, temperatureUnit)

        val radiationValue = Weather.getFormattedSolarRadiation(
            device.currentWeather?.solarIrradiance, includeUnit = false
        )
        setTextViewText(R.id.radiationValue, radiationValue)
        setTextViewText(R.id.radiationUnit, context.getString(R.string.solar_radiation_unit))

        setTextViewText(R.id.uvValue, Weather.getFormattedUV(device.currentWeather?.uvIndex))
    }
}
