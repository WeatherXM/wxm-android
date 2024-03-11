package com.weatherxm.util

import android.content.SharedPreferences
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ParametersBuilder
import com.google.firebase.analytics.logEvent
import com.weatherxm.BuildConfig
import com.weatherxm.R
import com.weatherxm.data.services.CacheService
import com.weatherxm.ui.common.DevicesFilterType
import com.weatherxm.ui.common.DevicesGroupBy
import com.weatherxm.ui.common.DevicesSortFilterOptions
import com.weatherxm.ui.common.DevicesSortOrder
import com.weatherxm.ui.common.empty
import timber.log.Timber

class Analytics(
    private val firebaseAnalytics: FirebaseAnalytics,
    private val cacheService: CacheService,
    private val displayModeHelper: DisplayModeHelper,
    preferences: SharedPreferences,
    private val resources: Resources
) {
    // Screen Names
    enum class Screen(val screenName: String) {
        SPLASH("Splash Screen"),
        ANALYTICS("Analytics Opt-In Prompt"),
        EXPLORER_LANDING("Explorer (Landing)"),
        EXPLORER("Explorer"),
        CLAIM_M5("Claim M5"),
        CLAIM_HELIUM("Claim Helium"),
        WALLET("Wallet"),
        DELETE_ACCOUNT("Delete Account"),
        DEVICE_ALERTS("Device Alerts"),
        HELIUM_OTA("OTA Update"),
        HISTORY("Device History"),
        DEVICES_LIST("Device List"),
        SETTINGS("App Settings"),
        LOGIN("Login"),
        SIGNUP("Sign Up"),
        PASSWORD_RESET("Password Reset"),
        PROFILE("Account"),
        CURRENT_WEATHER("Device Current Weather"),
        FORECAST("Device Forecast"),
        DEVICE_REWARDS("Device Rewards"),
        STATION_SETTINGS("Device Settings"),
        DEVICE_REWARD_TRANSACTIONS("Device Reward Transactions"),
        APP_UPDATE_PROMPT("App Update Prompt"),
        WIDGET_SELECT_STATION("Widget Station Selection"),
        CLAIM_DEVICE_TYPE_SELECTION("Claim Device Type Selection"),
        PASSWORD_CONFIRM("Password Confirm"),
        CHANGE_STATION_NAME("Change Station Name"),
        CHANGE_STATION_FREQUENCY("Change Station Frequency"),
        REBOOT_STATION("Reboot Station"),
        EXPLORER_CELL("Explorer Cell"),
        EXPLORER_DEVICE("Explorer Device"),
        BLE_CONNECTION_POPUP_ERROR("BLE Connection Popup Error"),
        NETWORK_STATS("Network Stats"),
        NETWORK_SEARCH("Network Search"),
        SORT_FILTER("Sort Filter"),
        DEVICE_REWARD_DETAILS("Device Rewards Details"),
        REWARD_ISSUES("Reward Issues"),
        REWARD_BOOST_DETAIL("Boost Detail"),
        DAILY_REWARD_INFO("Daily Reward info"),
        DATA_QUALITY_INFO("Data Quality info"),
        LOCATION_QUALITY_INFO("Location Quality info"),
        CELL_RANKING_INFO("Cell Ranking info"),
        CELL_CAPACITY_INFO("Cell Capacity info")
    }

    // Custom Event Names
    enum class CustomEvent(val eventName: String) {
        USER_ACTION("USER_ACTION"),
        VIEW_CONTENT("VIEW_CONTENT"),
        PROMPT("PROMPT")
    }

    // Custom Param Names
    enum class CustomParam(val paramName: String) {
        ACTION_NAME("ACTION_NAME"),
        ACTION("ACTION"),
        CONTENT_NAME("CONTENT_NAME"),
        CONTENT_ID("CONTENT_ID"),
        STEP("STEP"),
        PROMPT_NAME("PROMPT_NAME"),
        PROMPT_TYPE("PROMPT_TYPE"),
        STATE("STATE"),
        DATE("DATE"),
        FILTERS_SORT("SORT_BY"),
        FILTERS_FILTER("FILTER"),
        FILTERS_GROUP("GROUP_BY"),
        STATUS("STATUS")
    }

    // Custom Param Names
    enum class ParamValue(val paramValue: String) {
        SEARCH_LOCATION("Search Location"),
        CLAIMING_ADDRESS_SEARCH("Claiming Address Search"),
        SHARE_STATION_INFO("Share Station Information"),
        STATION_INFO("Station Information"),
        MY_LOCATION("My Location"),
        SELECT_DEVICE("Select Device"),
        USER_DEVICE_LIST("User Device List"),
        CLAIMING_RESULT("Claiming Result"),
        CLAIMING("Claiming"),
        CANCEL("Cancel"),
        RETRY("Retry"),
        VIEW_STATION("View Station"),
        CHANGE_STATION_NAME_RESULT("Change Station Name Result"),
        CHANGE_STATION_NAME("Change Station Name"),
        EDIT("Edit"),
        CLEAR("Clear"),
        CHANGE_FREQUENCY_RESULT("Change Frequency Result"),
        CHANGE_FREQUENCY("Change Station Frequency"),
        CHANGE("Change"),
        APP_UPDATE_PROMPT_RESULT("App Update Prompt Result"),
        APP_UPDATE_PROMPT("App Update Prompt"),
        UPDATE("Update"),
        DISCARD("Discard"),
        HELIUM_BLE_POPUP_ERROR("Helium BLE Popup Error"),
        HELIUM_BLE_POPUP("Helium BLE Popup"),
        QUIT("Quit"),
        TRY_AGAIN("Try Again"),
        LOGIN("Login"),
        LOGIN_ID("login"),
        EMAIL("email"),
        SIGNUP("Signup"),
        SIGNUP_ID("Signup"),
        SEND_EMAIL_FORGOT_PASSWORD("Send Email for Forgot Password"),
        SEND_EMAIL_FORGOT_PASSWORD_ID("forgot_password_email"),
        OTA_ERROR("OTA Error"),
        OTA_ERROR_ID("failure_ota"),
        SCAN("scan"),
        PAIR("pair"),
        CONNECT("connect"),
        DOWNLOAD("download"),
        INSTALL("install"),
        OTA_RESULT("OTA Result"),
        OTA_RESULT_ID("ota_result"),
        FAILURE("Failure"),
        FAILURE_ID("failure"),
        CLAIMING_RESULT_ID("claming_result"),
        CHANGE_STATION_NAME_RESULT_ID("change_station_name_result"),
        CHANGE_FREQUENCY_RESULT_ID("change_frequency_result"),
        WALLET_MISSING("Wallet Missing"),
        OTA_AVAILABLE("OTA Available"),
        LOW_BATTERY("Low Battery"),
        WALLET_COMPATIBILITY("Wallet Compatibility"),
        WARN("warn"),
        INFO("info"),
        VIEW("view"),
        DISMISS("dismiss"),
        ACTION("action"),
        FORECAST_DAY("Forecast Day"),
        OPEN("open"),
        CLOSE("close"),
        REMOVE_DEVICE("Remove Device"),
        CONTACT_SUPPORT("Contact Support"),
        DEVICE_ALERTS("device_alerts"),
        DEVICE_INFO("device_info"),
        SETTINGS("settings"),
        ERROR("error"),
        HISTORY_DAY("History Day"),
        WALLET_TRANSACTIONS("Wallet Transactions"),
        EDIT_WALLET("Edit Wallet"),
        CREATE_METAMASK("Create Metamask"),
        WALLET_TERMS("Wallet Terms Of Service"),
        WALLET_SCAN_QR("Scan QR Wallet"),
        LOGOUT("Logout"),
        DOCUMENTATION("Documentation"),
        ANNOUNCEMENTS("Announcements"),
        BLE_SCAN_AGAIN("BLE Scan Again"),
        DOCUMENTATION_FREQUENCY("Frequency Documentation"),
        OPEN_SHOP("Open Shop"),
        OPEN_STATION_SHOP("Open Station Shop"),
        TOTAL("total"),
        CLAIMED("claimed"),
        ACTIVE("active"),
        LEARN_MORE("Learn More"),
        DATA_DAYS("data_days"),
        ALLOCATED_REWARDS("allocated_rewards"),
        TOTAL_STATIONS("total_stations"),
        CLAIMED_STATIONS("claimed_stations"),
        ACTIVE_STATIONS("active_stations"),
        BUY_STATION("buy_station"),
        MANUFACTURER("Open Manufacturer Contact"),
        EXPLORER_SEARCH("Explorer Search"),
        EXPLORER_SETTINGS("Explorer Settings"),
        NETWORK_SEARCH("Network Search"),
        RECENT("recent"),
        SEARCH("search"),
        LOCATION("location"),
        STATION("station"),
        TOKENOMICS("Tokenomics"),
        NETWORK_STATS("network_stats"),
        DEVICE_DETAILS_FOLLOW("Device Details Follow"),
        DEVICE_DETAILS_SHARE("Device Details Share"),
        DEVICE_DETAILS_ADDRESS("Device Details Address"),
        DEVICE_LIST_FOLLOW("Device List Follow"),
        EXPLORER_DEVICE_LIST_FOLLOW("Explorer Device List Follow"),
        FOLLOW("follow"),
        UNFOLLOW("unfollow"),
        FILTERS("Filters"),
        FILTERS_SORT("sort_by"),
        FILTERS_FILTER("filter"),
        FILTERS_GROUP("group_by"),
        FILTERS_RESET("Filters Reset"),
        FILTERS_CANCEL("Filters Cancel"),
        FILTERS_SAVE("Filters Save"),
        FILTERS_SORT_DATE_ADDED("date_added"),
        FILTERS_SORT_NAME("name"),
        FILTERS_SORT_LAST_ACTIVE("last_active"),
        FILTERS_FILTER_ALL("all"),
        FILTERS_FILTER_OWNED("owned"),
        FILTERS_FILTER_FAVORITES("favorites"),
        FILTERS_GROUP_NO_GROUPING("no_grouping"),
        FILTERS_GROUP_RELATIONSHIP("relationship"),
        FILTERS_GROUP_STATUS("status"),
        IDENTIFY_PROBLEMS("Identify Problems"),
        REWARD_ISSUES_ERROR("Reward Issues Error"),
        NOTIFICATIONS("notifications"),
        ON("on"),
        OFF("off"),
        APP_SURVEY("App Survey"),
        USER_RESEARCH_PANEL("User Research Panel"),
        WEB_DOCUMENTATION("WEB_DOCUMENTATION"),
        INFO_DAILY_REWARDS("info_daily_rewards"),
        INFO_QOD("info_qod"),
        INFO_POL("info_pol"),
        INFO_CELL_POSITION("info_cell_position"),
        INFO_CELL_CAPACITY("info_cell_capacity")
    }

    // Custom Param Names
    enum class UserProperty(val propertyName: String) {
        THEME("theme"),
        UNIT_TEMPERATURE("UNIT_TEMPERATURE"),
        UNIT_WIND("UNIT_WIND"),
        UNIT_WIND_DIRECTION("UNIT_WIND_DIRECTION"),
        UNIT_PRECIPITATION("UNIT_PRECIPITATION"),
        UNIT_PRESSURE("UNIT_PRESSURE"),
        DARK("dark"),
        LIGHT("light"),
        SYSTEM("system"),
        CELSIUS("c"),
        FAHRENHEIT("f"),
        KMPH("kmph"),
        MPH("mph"),
        MPS("mps"),
        KN("kn"),
        BF("bf"),
        DEGREES("deg"),
        CARDINAL("card"),
        MILLIMETERS("mm"),
        INCHES("in"),
        HPA("hpa"),
        INHG("inhg"),
    }

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
        firebaseAnalytics.setUserId(cacheService.getUserId())

        // Selected Theme
        firebaseAnalytics.setUserProperty(
            UserProperty.THEME.propertyName,
            when (displayModeHelper.getDisplayMode()) {
                resources.getString(R.string.dark_value) -> UserProperty.DARK.propertyName
                resources.getString(R.string.light_value) -> UserProperty.LIGHT.propertyName
                else -> UserProperty.SYSTEM.propertyName
            }
        )

        // Selected Temperature Unit
        Weather.getPreferredUnit(
            resources.getString(CacheService.KEY_TEMPERATURE),
            resources.getString(R.string.temperature_celsius)
        ).let {
            firebaseAnalytics.setUserProperty(
                UserProperty.UNIT_TEMPERATURE.propertyName,
                if (it == resources.getString(R.string.temperature_celsius)) {
                    UserProperty.CELSIUS.propertyName
                } else {
                    UserProperty.FAHRENHEIT.propertyName
                }
            )
        }

        // Selected Wind Unit
        Weather.getPreferredUnit(
            resources.getString(CacheService.KEY_WIND),
            resources.getString(R.string.wind_speed_ms)
        ).let {
            firebaseAnalytics.setUserProperty(
                UserProperty.UNIT_WIND.propertyName,
                when (it) {
                    resources.getString(R.string.wind_speed_ms) -> UserProperty.MPS.propertyName
                    resources.getString(R.string.wind_speed_mph) -> UserProperty.MPH.propertyName
                    resources.getString(R.string.wind_speed_kmh) -> UserProperty.KMPH.propertyName
                    resources.getString(R.string.wind_speed_knots) -> UserProperty.KN.propertyName
                    else -> UserProperty.BF.propertyName
                }
            )
        }

        // Selected Wind Direction Unit
        Weather.getPreferredUnit(
            resources.getString(CacheService.KEY_WIND_DIR),
            resources.getString(R.string.wind_direction_cardinal)
        ).let {
            firebaseAnalytics.setUserProperty(
                UserProperty.UNIT_WIND_DIRECTION.propertyName,
                if (it == resources.getString(R.string.wind_direction_cardinal)) {
                    UserProperty.CARDINAL.propertyName
                } else {
                    UserProperty.DEGREES.propertyName
                }
            )
        }

        // Selected Precipitation Unit
        Weather.getPreferredUnit(
            resources.getString(CacheService.KEY_PRECIP),
            resources.getString(R.string.precipitation_mm)
        ).let {
            firebaseAnalytics.setUserProperty(
                UserProperty.UNIT_PRECIPITATION.propertyName,
                if (it == resources.getString(R.string.precipitation_mm)) {
                    UserProperty.MILLIMETERS.propertyName
                } else {
                    UserProperty.INCHES.propertyName
                }
            )
        }

        // Selected Pressure Unit
        Weather.getPreferredUnit(
            resources.getString(CacheService.KEY_PRESSURE),
            resources.getString(R.string.pressure_hpa)
        ).let {
            firebaseAnalytics.setUserProperty(
                UserProperty.UNIT_PRESSURE.propertyName,
                if (it == resources.getString(R.string.pressure_hpa)) {
                    UserProperty.HPA.propertyName
                } else {
                    UserProperty.INHG.propertyName
                }
            )
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
        firebaseAnalytics.setUserProperty(
            CustomParam.FILTERS_SORT.paramName,
            sortFilterOptions.getSortAnalyticsValue()
        )
        firebaseAnalytics.setUserProperty(
            CustomParam.FILTERS_FILTER.paramName,
            sortFilterOptions.getFilterAnalyticsValue()
        )
        firebaseAnalytics.setUserProperty(
            CustomParam.FILTERS_GROUP.paramName,
            sortFilterOptions.getGroupByAnalyticsValue()
        )
    }

    fun setAnalyticsEnabled(enabled: Boolean = cacheService.getAnalyticsEnabled()) {
        if (BuildConfig.DEBUG) {
            Timber.d("Skipping analytics tracking in DEBUG mode [enabled=$enabled].")
            firebaseAnalytics.setAnalyticsCollectionEnabled(false)
        } else {
            Timber.d("Resetting analytics tracking [enabled=$enabled]")
            firebaseAnalytics.setAnalyticsCollectionEnabled(enabled)
        }
    }

    fun trackScreen(screen: Screen, screenClass: String?, itemId: String? = null) {
        if (cacheService.getAnalyticsEnabled()) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
                param(FirebaseAnalytics.Param.SCREEN_NAME, screen.screenName)
                param(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass ?: String.empty())
                itemId?.let { param(FirebaseAnalytics.Param.ITEM_ID, itemId) }
            }
        }
    }

    fun trackScreen(screenName: String, screenClass: String?) {
        if (cacheService.getAnalyticsEnabled()) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
                param(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
                param(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass ?: String.empty())
            }
        }
    }

    fun trackEventUserAction(
        actionName: String,
        contentType: String? = null,
        vararg customParams: Pair<String, String>
    ) {
        if (!cacheService.getAnalyticsEnabled()) return

        val params = ParametersBuilder().apply {
            param(CustomParam.ACTION_NAME.paramName, actionName)

            contentType?.let {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, contentType)
            }

            customParams.forEach {
                param(it.first, it.second)
            }
        }
        firebaseAnalytics.logEvent(CustomEvent.USER_ACTION.eventName, params.bundle)
    }

    fun trackEventViewContent(
        contentName: String,
        contentId: String,
        vararg customParams: Pair<String, String>,
        success: Long? = null
    ) {
        if (!cacheService.getAnalyticsEnabled()) return

        val params = ParametersBuilder().apply {
            param(CustomParam.CONTENT_NAME.paramName, contentName)
            param(CustomParam.CONTENT_ID.paramName, contentId)

            customParams.forEach {
                param(it.first, it.second)
            }
            success?.let {
                param(FirebaseAnalytics.Param.SUCCESS, it)
            }
        }
        firebaseAnalytics.logEvent(CustomEvent.VIEW_CONTENT.eventName, params.bundle)
    }

    fun trackEventFailure(failureId: String?) {
        trackEventViewContent(
            ParamValue.FAILURE.paramValue,
            ParamValue.FAILURE_ID.paramValue,
            Pair(FirebaseAnalytics.Param.ITEM_ID, failureId ?: String.empty())
        )
    }

    fun trackEventPrompt(
        promptName: String,
        promptType: String,
        action: String,
        vararg customParams: Pair<String, String>
    ) {
        if (!cacheService.getAnalyticsEnabled()) return

        val params = ParametersBuilder().apply {
            param(CustomParam.PROMPT_NAME.paramName, promptName)
            param(CustomParam.PROMPT_TYPE.paramName, promptType)
            param(CustomParam.ACTION.paramName, action)

            customParams.forEach {
                param(it.first, it.second)
            }
        }
        firebaseAnalytics.logEvent(CustomEvent.PROMPT.eventName, params.bundle)
    }

    fun trackEventSelectContent(
        contentType: String,
        vararg customParams: Pair<String, String>,
        index: Long? = null
    ) {
        if (!cacheService.getAnalyticsEnabled()) return

        val params = ParametersBuilder().apply {
            param(FirebaseAnalytics.Param.CONTENT_TYPE, contentType)

            customParams.forEach {
                param(it.first, it.second)
            }

            index?.let {
                param(FirebaseAnalytics.Param.INDEX, it)
            }
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, params.bundle)
    }
}
