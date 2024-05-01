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

class AnalyticsImpl(
    private val firebaseLib: FirebaseAnalyticsLib,
    private val mixpanelLib: MixpanelLib,
    private val cacheService: CacheService,
    private val displayModeHelper: DisplayModeHelper,
    preferences: SharedPreferences,
    private val context: Context
) : Analytics {

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
    override fun setUserProperties() {
        val userParams = mutableListOf<Pair<String, String>>()

        // Selected Theme
        when (displayModeHelper.getDisplayMode()) {
            context.getString(R.string.dark_value) -> Analytics.UserProperty.DARK.propertyName
            context.getString(R.string.light_value) -> Analytics.UserProperty.LIGHT.propertyName
            else -> Analytics.UserProperty.SYSTEM.propertyName
        }.apply {
            userParams.add(Pair(Analytics.UserProperty.THEME.propertyName, this))
        }

        // Selected Temperature Unit
        Weather.getPreferredUnit(
            context.getString(CacheService.KEY_TEMPERATURE),
            context.getString(R.string.temperature_celsius)
        ).let {
            if (it == context.getString(R.string.temperature_celsius)) {
                Analytics.UserProperty.CELSIUS.propertyName
            } else {
                Analytics.UserProperty.FAHRENHEIT.propertyName
            }.apply {
                userParams.add(Pair(Analytics.UserProperty.UNIT_TEMPERATURE.propertyName, this))
            }
        }

        // Selected Wind Unit
        Weather.getPreferredUnit(
            context.getString(CacheService.KEY_WIND),
            context.getString(R.string.wind_speed_ms)
        ).let {
            when (it) {
                context.getString(R.string.wind_speed_ms) -> Analytics.UserProperty.MPS.propertyName
                context.getString(R.string.wind_speed_mph) -> {
                    Analytics.UserProperty.MPH.propertyName
                }
                context.getString(R.string.wind_speed_kmh) -> {
                    Analytics.UserProperty.KMPH.propertyName
                }
                context.getString(R.string.wind_speed_knots) -> {
                    Analytics.UserProperty.KN.propertyName
                }
                else -> Analytics.UserProperty.BF.propertyName
            }.apply {
                userParams.add(Pair(Analytics.UserProperty.UNIT_WIND.propertyName, this))
            }
        }

        // Selected Wind Direction Unit
        Weather.getPreferredUnit(
            context.getString(CacheService.KEY_WIND_DIR),
            context.getString(R.string.wind_direction_cardinal)
        ).let {
            if (it == context.getString(R.string.wind_direction_cardinal)) {
                Analytics.UserProperty.CARDINAL.propertyName
            } else {
                Analytics.UserProperty.DEGREES.propertyName
            }.apply {
                userParams.add(Pair(Analytics.UserProperty.UNIT_WIND_DIRECTION.propertyName, this))
            }
        }

        // Selected Precipitation Unit
        Weather.getPreferredUnit(
            context.getString(CacheService.KEY_PRECIP),
            context.getString(R.string.precipitation_mm)
        ).let {
            if (it == context.getString(R.string.precipitation_mm)) {
                Analytics.UserProperty.MILLIMETERS.propertyName
            } else {
                Analytics.UserProperty.INCHES.propertyName
            }.apply {
                userParams.add(Pair(Analytics.UserProperty.UNIT_PRECIPITATION.propertyName, this))
            }
        }

        // Selected Pressure Unit
        Weather.getPreferredUnit(
            context.getString(CacheService.KEY_PRESSURE),
            context.getString(R.string.pressure_hpa)
        ).let {
            if (it == context.getString(R.string.pressure_hpa)) {
                Analytics.UserProperty.HPA.propertyName
            } else {
                Analytics.UserProperty.INHG.propertyName
            }.apply {
                userParams.add(Pair(Analytics.UserProperty.UNIT_PRESSURE.propertyName, this))
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
                Pair(Analytics.CustomParam.FILTERS_SORT.paramName, getSortAnalyticsValue())
            )
            userParams.add(
                Pair(Analytics.CustomParam.FILTERS_FILTER.paramName, getFilterAnalyticsValue())
            )
            userParams.add(
                Pair(Analytics.CustomParam.FILTERS_GROUP.paramName, getGroupByAnalyticsValue())
            )
        }

        val userId = cacheService.getUserId()
        firebaseLib.setUserProperties(userId, userParams)
        mixpanelLib.setUserProperties(userId, userParams)
    }

    override fun setAnalyticsEnabled(enabled: Boolean) {
        firebaseLib.setAnalyticsEnabled(enabled)
        mixpanelLib.setAnalyticsEnabled(enabled)
    }

    override fun trackScreen(screen: Analytics.Screen, screenClass: String, itemId: String?) {
        if (cacheService.getAnalyticsEnabled()) {
            firebaseLib.trackScreen(screen.screenName, screenClass, itemId)
            mixpanelLib.trackScreen(screenClass, itemId)
        }
    }

    override fun trackScreen(screenName: String, screenClass: String) {
        if (cacheService.getAnalyticsEnabled()) {
            firebaseLib.trackScreen(screenName, screenClass)
            mixpanelLib.trackScreen(screenClass)
        }
    }

    override fun trackEventUserAction(
        actionName: String,
        contentType: String?,
        vararg customParams: Pair<String, String>
    ) {
        if (cacheService.getAnalyticsEnabled()) {
            firebaseLib.trackEventUserAction(actionName, contentType, *customParams)
            mixpanelLib.trackEventUserAction(actionName, contentType, *customParams)
        }
    }

    override fun trackEventViewContent(
        contentName: String,
        contentId: String,
        vararg customParams: Pair<String, String>,
        success: Long?
    ) {
        if (cacheService.getAnalyticsEnabled()) {
            firebaseLib.trackEventViewContent(
                contentName, contentId, *customParams, success = success
            )
            mixpanelLib.trackEventViewContent(
                contentName, contentId, *customParams, success = success
            )
        }
    }

    override fun trackEventFailure(failureId: String?) {
        trackEventViewContent(
            Analytics.ParamValue.FAILURE.paramValue,
            Analytics.ParamValue.FAILURE_ID.paramValue,
            Pair(FirebaseAnalytics.Param.ITEM_ID, failureId ?: String.empty())
        )
    }

    override fun trackEventPrompt(
        promptName: String,
        promptType: String,
        action: String,
        vararg customParams: Pair<String, String>
    ) {
        if (cacheService.getAnalyticsEnabled()) {
            firebaseLib.trackEventPrompt(promptName, promptType, action, *customParams)
            mixpanelLib.trackEventPrompt(promptName, promptType, action, *customParams)
        }
    }

    override fun trackEventSelectContent(
        contentType: String,
        vararg customParams: Pair<String, String>,
        index: Long?
    ) {
        if (cacheService.getAnalyticsEnabled()) {
            firebaseLib.trackEventSelectContent(contentType, *customParams, index = index)
            mixpanelLib.trackEventSelectContent(contentType, *customParams, index = index)
        }
    }
}
