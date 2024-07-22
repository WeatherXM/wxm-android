package com.weatherxm.analytics

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import com.weatherxm.R
import com.weatherxm.data.services.CacheService
import com.weatherxm.ui.common.DevicesFilterType
import com.weatherxm.ui.common.DevicesGroupBy
import com.weatherxm.ui.common.DevicesSortFilterOptions
import com.weatherxm.ui.common.DevicesSortOrder
import com.weatherxm.ui.common.empty
import com.weatherxm.util.Weather

class AnalyticsWrapper(
    private var analytics: List<AnalyticsService>,
    private val context: Context
) {
    private var areAnalyticsEnabled: Boolean = false
    private var displayMode: String = AnalyticsService.UserProperty.SYSTEM.propertyName
    private var devicesSortFilterOptions: List<String> = mutableListOf()
    private var devicesOwn: Int = 0
    private var hasWallet: Boolean = false

    fun setDevicesOwn(devicesOwn: Int) {
        this.devicesOwn = devicesOwn
    }

    fun setHasWallet(hasWallet: Boolean) {
        this.hasWallet = hasWallet
    }

    fun setUserId(userId: String) {
        if (userId.isNotEmpty()) {
            analytics.forEach { it.setUserId(userId) }
        }
    }

    fun setDevicesSortFilterOptions(options: List<String>) {
        this.devicesSortFilterOptions = options
    }

    fun setDisplayMode(displayMode: String) {
        this.displayMode = displayMode
    }

    // Suppress CyclomaticComplexMethod because it is just a bunch of if/when statements.
    @Suppress("CyclomaticComplexMethod", "LongMethod")
    fun setUserProperties(): List<Pair<String, String>> {
        val userParams = mutableListOf<Pair<String, String>>()

        userParams.add(Pair(AnalyticsService.UserProperty.THEME.propertyName, displayMode))

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

        val sortFilterOptions = if (devicesSortFilterOptions.isEmpty()) {
            DevicesSortFilterOptions()
        } else {
            DevicesSortFilterOptions(
                DevicesSortOrder.valueOf(devicesSortFilterOptions[0]),
                DevicesFilterType.valueOf(devicesSortFilterOptions[1]),
                DevicesGroupBy.valueOf(devicesSortFilterOptions[2])
            )
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

        userParams.add(
            Pair(AnalyticsService.UserProperty.STATIONS_OWN.propertyName, devicesOwn.toString())
        )

        userParams.add(
            Pair(AnalyticsService.UserProperty.HAS_WALLET.propertyName, hasWallet.toString())
        )

        analytics.forEach { it.setUserProperties(userParams) }
        return userParams
    }

    fun setAnalyticsEnabled(enabled: Boolean) {
        areAnalyticsEnabled = enabled
        analytics.forEach { it.setAnalyticsEnabled(enabled) }
    }

    fun getAnalyticsEnabled() = areAnalyticsEnabled

    fun onLogout() {
        analytics.forEach { it.onLogout() }
    }

    fun trackScreen(screen: AnalyticsService.Screen, screenClass: String, itemId: String? = null) {
        if (areAnalyticsEnabled) {
            analytics.forEach { it.trackScreen(screen, screenClass, itemId) }
        }
    }

    fun trackEventUserAction(
        actionName: String,
        contentType: String? = null,
        vararg customParams: Pair<String, String>
    ) {
        if (areAnalyticsEnabled) {
            analytics.forEach { it.trackEventUserAction(actionName, contentType, *customParams) }
        }
    }

    fun trackEventViewContent(
        contentName: String,
        contentId: String?,
        vararg customParams: Pair<String, String>,
        success: Long? = null
    ) {
        if (areAnalyticsEnabled) {
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
        if (areAnalyticsEnabled) {
            analytics.forEach { it.trackEventPrompt(promptName, promptType, action, *customParams) }
        }
    }

    fun trackEventSelectContent(
        contentType: String,
        vararg customParams: Pair<String, String>,
        index: Long? = null
    ) {
        if (areAnalyticsEnabled) {
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
