package com.weatherxm.ui.widgets

import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import androidx.core.graphics.drawable.toBitmap
import com.weatherxm.R
import com.weatherxm.data.services.CacheService.Companion.KEY_PRESSURE
import com.weatherxm.data.services.CacheService.Companion.KEY_TEMPERATURE
import com.weatherxm.data.services.CacheService.Companion.KEY_WIND
import com.weatherxm.ui.common.Contracts
import com.weatherxm.ui.common.DeviceRelation
import com.weatherxm.ui.common.UIDevice
import com.weatherxm.ui.devicedetails.DeviceDetailsActivity
import com.weatherxm.ui.login.LoginActivity
import com.weatherxm.ui.widgets.selectstation.SelectStationActivity
import com.weatherxm.util.AndroidBuildInfo
import com.weatherxm.util.DateTimeHelper.getFormattedDate
import com.weatherxm.util.DateTimeHelper.getFormattedTime
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
        appWidgetId,
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
        if (AndroidBuildInfo.sdkInt >= Build.VERSION_CODES.S) {
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

    if (device.isHelium()) {
        setImageViewResource(R.id.bundleIcon, R.drawable.ic_helium)
    } else if (device.isWifi()) {
        setImageViewResource(R.id.bundleIcon, R.drawable.ic_wifi)
    } else if (device.isCellular()) {
        setImageViewResource(R.id.bundleIcon, R.drawable.ic_cellular)
    }
    setTextViewText(R.id.bundleName, device.bundleTitle)

    this.setStatus(context, device)

    if (device.currentWeather == null || device.currentWeather.isEmpty()) {
        setNoData(context, device)
    } else {
        setViewVisibility(R.id.noDataLayout, View.GONE)
        setInt(R.id.root, "setBackgroundResource", R.drawable.background_rounded_surface)
        setViewPadding(R.id.root, 0, 0, 0, 0)
        this.setWeatherData(context, device, widgetType)
    }

    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, this)
}

fun RemoteViews.setNoData(context: Context, device: UIDevice) {
    setViewVisibility(R.id.weatherDataLayout, View.GONE)
    setViewVisibility(R.id.noDataLayout, View.VISIBLE)

    if (!device.isOwned()) {
        setTextViewText(
            R.id.noDataMessage,
            context.getString(R.string.no_data_message_public_device)
        )
    } else {
        setTextViewText(R.id.noDataMessage, context.getString(R.string.no_data_message))
    }

    setViewPadding(R.id.root, 2, 2, 2, 2)
    val backgroundResId = R.drawable.background_rounded_surface_error_stroke
    setInt(R.id.root, "setBackgroundResource", backgroundResId)
}

fun RemoteViews.setStatus(context: Context, device: UIDevice) {
    val lastSeen = if (device.lastWeatherStationActivity?.isToday() == true || device.isOnline()) {
        device.lastWeatherStationActivity?.getFormattedTime(context)
    } else {
        device.lastWeatherStationActivity?.getFormattedDate()
    }
    setTextViewText(R.id.lastSeen, lastSeen)

    when (device.isActive) {
        true -> {
            setInt(R.id.statusIcon, "setColorFilter", context.getColor(R.color.success))
            setInt(
                R.id.statusContainer,
                "setBackgroundResource",
                R.drawable.background_rounded_success
            )
        }
        false -> {
            setInt(R.id.statusIcon, "setColorFilter", context.getColor(R.color.error))
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
    val windDirection = device.currentWeather?.windDirection
    val windDirectionUnit = Weather.getFormattedWindDirection(windDirection)
    val windDirectionDrawable = Weather.getWindDirectionDrawable(context, windDirection)
    if (widgetType != WidgetType.CURRENT_WEATHER_TILE) {
        val humidity = Weather.getFormattedHumidity(device.currentWeather?.humidity, false)
        setTextViewText(R.id.humidityValue, humidity)
        setTextViewText(R.id.humidityUnit, "%")

        val windValue = Weather.getFormattedWind(
            device.currentWeather?.windSpeed, windDirection, includeUnits = false
        )
        setTextViewText(R.id.windValue, windValue)
        setTextViewText(R.id.windUnit, "$windUnit $windDirectionUnit")
        setImageViewBitmap(R.id.windIconDirection, windDirectionDrawable?.toBitmap())

        setRelationIcon(device.relation)

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
            device.currentWeather?.windGust, windDirection, includeUnits = false
        )
        setTextViewText(R.id.windGustValue, windGustValue)
        setTextViewText(R.id.windGustUnit, "$windUnit $windDirectionUnit")
        setImageViewBitmap(R.id.windGustIconDirection, windDirectionDrawable?.toBitmap())

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

private fun RemoteViews.setRelationIcon(relation: DeviceRelation?) {
    @Suppress("UseCheckOrError")
    setImageViewResource(
        R.id.stationHomeFollowIcon,
        when (relation) {
            DeviceRelation.OWNED -> R.drawable.ic_home
            DeviceRelation.FOLLOWED -> R.drawable.ic_favorite
            DeviceRelation.UNFOLLOWED -> R.drawable.ic_favorite_outline
            null -> throw IllegalStateException("Oops! No device relation here.")
        }
    )
}
