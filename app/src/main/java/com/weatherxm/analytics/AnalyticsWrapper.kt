package com.weatherxm.analytics

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.analytics.FirebaseAnalytics
import com.weatherxm.R
import com.weatherxm.data.services.CacheService
import com.weatherxm.ui.common.DevicesFilterType
import com.weatherxm.ui.common.DevicesGroupBy
import com.weatherxm.ui.common.DevicesSortFilterOptions
import com.weatherxm.ui.common.DevicesSortOrder
import com.weatherxm.ui.common.empty
import com.weatherxm.util.DisplayModeHelper
import com.weatherxm.util.Weather
import timber.log.Timber

class AnalyticsWrapper(
    private var analytics: List<AnalyticsService>,
    private val cacheService: CacheService,
    private val displayModeHelper: DisplayModeHelper,
    preferences: SharedPreferences,
    private val context: Context
) {

    private val onPreferencesChanged = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        setUserProperties()
    }

    init {
        preferences.registerOnSharedPreferenceChangeListener(onPreferencesChanged)
        val enabled = cacheService.getAnalyticsEnabled()
        Timber.d("Initializing Analytics [enabled=$enabled]")
        setUserProperties()
        setAnalyticsEnabled(enabled)
    }

    // Suppress CyclomaticComplexMethod because it is just a bunch of if/when statements.
    @Suppress("CyclomaticComplexMethod", "LongMethod")
    private fun setUserProperties() {
        val userParams = mutableListOf<Pair<String, String>>()

        // Selected Theme
        when (displayModeHelper.getDisplayMode()) {
            context.getString(R.string.dark_value) -> {
                AnalyticsService.UserProperty.DARK.propertyName
            }
            context.getString(R.string.light_value) -> {
                AnalyticsService.UserProperty.LIGHT.propertyName
            }
            else -> AnalyticsService.UserProperty.SYSTEM.propertyName
        }.apply {
            userParams.add(Pair(AnalyticsService.UserProperty.THEME.propertyName, this))
        }

        // Selected Temperature Unit
        Weather.getPreferredUnit(
            context.getString(CacheService.KEY_TEMPERATURE),
            context.getString(R.string.temperature_celsius)
        ).let {
            if (it == context.getString(R.string.temperature_celsius)) {
                AnalyticsService.UserProperty.CELSIUS.propertyName
            } else {
                AnalyticsService.UserProperty.FAHRENHEIT.propertyName
            }.apply {
                userParams.add(
                    Pair(AnalyticsService.UserProperty.UNIT_TEMPERATURE.propertyName, this)
                )
            }
        }

        // Selected Wind Unit
        Weather.getPreferredUnit(
            context.getString(CacheService.KEY_WIND),
            context.getString(R.string.wind_speed_ms)
        ).let {
            when (it) {
                context.getString(R.string.wind_speed_ms) -> {
                    AnalyticsService.UserProperty.MPS.propertyName
                }
                context.getString(R.string.wind_speed_mph) -> {
                    AnalyticsService.UserProperty.MPH.propertyName
                }
                context.getString(R.string.wind_speed_kmh) -> {
                    AnalyticsService.UserProperty.KMPH.propertyName
                }
                context.getString(R.string.wind_speed_knots) -> {
                    AnalyticsService.UserProperty.KN.propertyName
                }
                else -> AnalyticsService.UserProperty.BF.propertyName
            }.apply {
                userParams.add(Pair(AnalyticsService.UserProperty.UNIT_WIND.propertyName, this))
            }
        }

        // Selected Wind Direction Unit
        Weather.getPreferredUnit(
            context.getString(CacheService.KEY_WIND_DIR),
            context.getString(R.string.wind_direction_cardinal)
        ).let {
            if (it == context.getString(R.string.wind_direction_cardinal)) {
                AnalyticsService.UserProperty.CARDINAL.propertyName
            } else {
                AnalyticsService.UserProperty.DEGREES.propertyName
            }.apply {
                userParams.add(
                    Pair(AnalyticsService.UserProperty.UNIT_WIND_DIRECTION.propertyName, this)
                )
            }
        }

        // Selected Precipitation Unit
        Weather.getPreferredUnit(
            context.getString(CacheService.KEY_PRECIP),
            context.getString(R.string.precipitation_mm)
        ).let {
            if (it == context.getString(R.string.precipitation_mm)) {
                AnalyticsService.UserProperty.MILLIMETERS.propertyName
            } else {
                AnalyticsService.UserProperty.INCHES.propertyName
            }.apply {
                userParams.add(
                    Pair(AnalyticsService.UserProperty.UNIT_PRECIPITATION.propertyName, this)
                )
            }
        }

        // Selected Pressure Unit
        Weather.getPreferredUnit(
            context.getString(CacheService.KEY_PRESSURE),
            context.getString(R.string.pressure_hpa)
        ).let {
            if (it == context.getString(R.string.pressure_hpa)) {
                AnalyticsService.UserProperty.HPA.propertyName
            } else {
                AnalyticsService.UserProperty.INHG.propertyName
            }.apply {
                userParams.add(Pair(AnalyticsService.UserProperty.UNIT_PRESSURE.propertyName, this))
            }
        }

        val sortFilterOptions = cacheService.getDevicesSortFilterOptions().let {
            if (it.isEmpty()) {
                DevicesSortFilterOptions()
            } else {
                DevicesSortFilterOptions(
                    DevicesSortOrder.valueOf(it[0]),
                    DevicesFilterType.valueOf(it[1]),
                    DevicesGroupBy.valueOf(it[2])
                )
            }
        }
        with(sortFilterOptions) {
            userParams.add(
                Pair(AnalyticsService.CustomParam.FILTERS_SORT.paramName, getSortAnalyticsValue())
            )
            userParams.add(
                Pair(
                    AnalyticsService.CustomParam.FILTERS_FILTER.paramName, getFilterAnalyticsValue()
                )
            )
            userParams.add(
                Pair(
                    AnalyticsService.CustomParam.FILTERS_GROUP.paramName, getGroupByAnalyticsValue()
                )
            )
        }

        val userId = cacheService.getUserId()
        analytics.forEach { it.setUserProperties(userId, userParams) }
    }

    fun setAnalyticsEnabled(enabled: Boolean) {
        analytics.forEach { it.setAnalyticsEnabled(enabled) }
    }

    fun trackScreen(screen: AnalyticsService.Screen, screenClass: String, itemId: String? = null) {
        if (cacheService.getAnalyticsEnabled()) {
            analytics.forEach { it.trackScreen(screen, screenClass, itemId) }
        }
    }

    fun trackEventUserAction(
        actionName: String,
        contentType: String? = null,
        vararg customParams: Pair<String, String>
    ) {
        if (cacheService.getAnalyticsEnabled()) {
            analytics.forEach { it.trackEventUserAction(actionName, contentType, *customParams) }
        }
    }

    fun trackEventViewContent(
        contentName: String,
        contentId: String?,
        vararg customParams: Pair<String, String>,
        success: Long? = null
    ) {
        if (cacheService.getAnalyticsEnabled()) {
            analytics.forEach {
                it.trackEventViewContent(
                    contentName,
                    contentId,
                    *customParams,
                    success = success
                )
            }
        }
    }

    fun trackEventFailure(failureId: String?) {
        trackEventViewContent(
            AnalyticsService.ParamValue.FAILURE.paramValue,
            AnalyticsService.ParamValue.FAILURE_ID.paramValue,
            Pair(FirebaseAnalytics.Param.ITEM_ID, failureId ?: String.empty())
        )
    }

    fun trackEventPrompt(
        promptName: String,
        promptType: String,
        action: String,
        vararg customParams: Pair<String, String>
    ) {
        if (cacheService.getAnalyticsEnabled()) {
            analytics.forEach { it.trackEventPrompt(promptName, promptType, action, *customParams) }
        }
    }

    fun trackEventSelectContent(
        contentType: String,
        vararg customParams: Pair<String, String>,
        index: Long? = null
    ) {
        if (cacheService.getAnalyticsEnabled()) {
            analytics.forEach {
                it.trackEventSelectContent(
                    contentType,
                    *customParams,
                    index = index
                )
            }
        }
    }
}
