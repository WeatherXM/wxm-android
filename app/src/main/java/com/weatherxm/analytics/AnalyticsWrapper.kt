package com.weatherxm.analytics

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import com.weatherxm.ui.common.DevicesFilterType
import com.weatherxm.ui.common.DevicesGroupBy
import com.weatherxm.ui.common.DevicesSortFilterOptions
import com.weatherxm.ui.common.DevicesSortOrder
import com.weatherxm.ui.common.WeatherUnitType
import com.weatherxm.ui.common.empty
import com.weatherxm.util.UnitSelector

// Suppress it as it's just a bunch of set/get functions
@Suppress("TooManyFunctions")
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
        UnitSelector.getTemperatureUnit(context).apply {
            val analyticsValue = when (this.type) {
                WeatherUnitType.CELSIUS -> AnalyticsService.UserProperty.CELSIUS.propertyName
                WeatherUnitType.FAHRENHEIT -> AnalyticsService.UserProperty.FAHRENHEIT.propertyName
                else -> AnalyticsService.UserProperty.CELSIUS.propertyName
            }

            userParams.add(
                Pair(AnalyticsService.UserProperty.UNIT_TEMPERATURE.propertyName, analyticsValue)
            )
        }

        // Selected Wind Unit
        UnitSelector.getWindUnit(context).apply {
            val analyticsValue = when (this.type) {
                WeatherUnitType.MS -> AnalyticsService.UserProperty.MPS.propertyName
                WeatherUnitType.MPH -> AnalyticsService.UserProperty.MPH.propertyName
                WeatherUnitType.KMH -> AnalyticsService.UserProperty.KMPH.propertyName
                WeatherUnitType.KNOTS -> AnalyticsService.UserProperty.KN.propertyName
                WeatherUnitType.BEAUFORT -> AnalyticsService.UserProperty.BF.propertyName
                else -> AnalyticsService.UserProperty.MPS.propertyName
            }

            userParams.add(
                Pair(AnalyticsService.UserProperty.UNIT_WIND.propertyName, analyticsValue)
            )
        }

        // Selected Wind Direction Unit
        UnitSelector.getWindDirectionUnit(context).apply {
            val analyticsValue = when (this.type) {
                WeatherUnitType.CARDINAL -> AnalyticsService.UserProperty.CARDINAL.propertyName
                WeatherUnitType.DEGREES -> AnalyticsService.UserProperty.DEGREES.propertyName
                else -> AnalyticsService.UserProperty.CARDINAL.propertyName
            }

            userParams.add(
                Pair(AnalyticsService.UserProperty.UNIT_WIND_DIRECTION.propertyName, analyticsValue)
            )
        }

        // Selected Precipitation Unit
        UnitSelector.getPrecipitationUnit(context, false).apply {
            val analyticsValue = when (this.type) {
                WeatherUnitType.MILLIMETERS -> {
                    AnalyticsService.UserProperty.MILLIMETERS.propertyName
                }
                WeatherUnitType.INCHES -> AnalyticsService.UserProperty.INCHES.propertyName
                else -> AnalyticsService.UserProperty.MILLIMETERS.propertyName
            }

            userParams.add(
                Pair(AnalyticsService.UserProperty.UNIT_PRECIPITATION.propertyName, analyticsValue)
            )
        }

        // Selected Pressure Unit
        UnitSelector.getPressureUnit(context).apply {
            val analyticsValue = when (this.type) {
                WeatherUnitType.HPA -> AnalyticsService.UserProperty.HPA.propertyName
                WeatherUnitType.INHG -> AnalyticsService.UserProperty.INHG.propertyName
                else -> AnalyticsService.UserProperty.HPA.propertyName
            }

            userParams.add(
                Pair(AnalyticsService.UserProperty.UNIT_PRESSURE.propertyName, analyticsValue)
            )
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
