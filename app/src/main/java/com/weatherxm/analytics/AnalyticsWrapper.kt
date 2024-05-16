package com.weatherxm.analytics

import com.google.firebase.analytics.FirebaseAnalytics
import com.weatherxm.ui.common.DevicesFilterType
import com.weatherxm.ui.common.DevicesGroupBy
import com.weatherxm.ui.common.DevicesSortFilterOptions
import com.weatherxm.ui.common.DevicesSortOrder
import com.weatherxm.ui.common.empty

class AnalyticsWrapper(private var analytics: List<AnalyticsService>) {
    private var areAnalyticsEnabled: Boolean = false
    private var userId: String = String.empty()
    private var displayMode: String = AnalyticsService.UserProperty.SYSTEM.propertyName
    private var devicesSortFilterOptions: List<String> = mutableListOf()

    fun setUserId(userId: String) {
        this.userId = userId
    }

    fun setDevicesSortFilterOptions(options: List<String>) {
        this.devicesSortFilterOptions = options
    }

    fun setDisplayMode(displayMode: String) {
        this.displayMode = displayMode
    }

    fun setUserProperties(weatherUnits: List<Pair<String, String>>) {
        val userParams = mutableListOf<Pair<String, String>>()

        userParams.add(Pair(AnalyticsService.UserProperty.THEME.propertyName, displayMode))

        userParams.addAll(weatherUnits)

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

        analytics.forEach { it.setUserProperties(userId, userParams) }
    }

    fun setAnalyticsEnabled(enabled: Boolean) {
        areAnalyticsEnabled = enabled
        analytics.forEach { it.setAnalyticsEnabled(enabled) }
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
        contentId: String,
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
