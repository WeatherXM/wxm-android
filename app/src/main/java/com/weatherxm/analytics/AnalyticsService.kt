package com.weatherxm.analytics

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

interface AnalyticsService {
    // Some event key names
    enum class EventKey(val key: String) {
        SCREEN_VIEW("screen_view"),
        SCREEN_NAME("screen_name"),
        ITEM_ID("item_id"),
        CONTENT_TYPE("content_type"),
        SUCCESS("success"),
        INDEX("index"),
        SELECT_CONTENT("select_content")
    }

    // Screen Names
    @Parcelize
    enum class Screen(val screenName: String) : Parcelable {
        ANALYTICS("Analytics Opt-In Prompt"),
        EXPLORER_LANDING("Explorer (Landing)"),
        EXPLORER("Explorer"),
        CLAIM_M5("Claim M5"),
        CLAIM_PULSE("Claim Pulse"),
        CLAIM_HELIUM("Claim Helium"),
        CLAIM_D1("Claim D1"),
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
        DEVICE_FORECAST("Device Forecast"),
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
        CELL_CAPACITY_INFO("Cell Capacity info"),
        DEVICE_FORECAST_DETAILS("Device Forecast Details"),
        CLAIM_DAPP("Claim Dapp"),
        TEMPERATURE_BARS_EXPLANATION("Temperature Bars Explanation"),
        REWARD_ANALYTICS("Reward Analytics"),
        STATION_PHOTOS_INSTRUCTIONS("Station Photos Instructions"),
        STATION_PHOTOS_INTRO("Station Photos Intro"),
        STATION_PHOTOS_GALLERY("Station Photos Gallery")
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
        STEP("STEP"),
        PROMPT_NAME("PROMPT_NAME"),
        PROMPT_TYPE("PROMPT_TYPE"),
        DATE("DATE"),
        FILTERS_SORT("SORT_BY"),
        FILTERS_FILTER("FILTER"),
        FILTERS_GROUP("GROUP_BY"),
        STATUS("STATUS"),
        STATE("STATE"),
        APP_ID("APP_ID"),
        DEVICE_STATE("DEVICE_STATE"),
        USER_STATE("USER_STATE")
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
        UPDATE_STATION("Update Station"),
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
        TRY_AGAIN("Try Again"),
        LOGIN("Login"),
        EMAIL("email"),
        SIGNUP("Signup"),
        SEND_EMAIL_FORGOT_PASSWORD("Send Email for Forgot Password"),
        OTA_ERROR("OTA Error"),
        SCAN("scan"),
        PAIR("pair"),
        CONNECT("connect"),
        DOWNLOAD("download"),
        INSTALL("install"),
        OTA_RESULT("OTA Result"),
        FAILURE("Failure"),
        SUCCESS_ID("success"),
        FAILURE_ID("failure"),
        WALLET_MISSING("Wallet Missing"),
        OTA_AVAILABLE("OTA Available"),
        LOW_BATTERY("Low Battery"),
        WALLET_COMPATIBILITY("Wallet Compatibility"),
        WARN("warn"),
        INFO("info"),
        VIEW("view"),
        DISMISS("dismiss"),
        ACTION("action"),
        REMOVE_DEVICE("Remove Device"),
        CONTACT_SUPPORT("Contact Support"),
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
        DEVICE_DETAILS_FOLLOW("Device Details Follow"),
        DEVICE_DETAILS_SHARE("Device Details Share"),
        DEVICE_LIST_FOLLOW("Devices List Follow"),
        EXPLORER_DEVICE_LIST_FOLLOW("Explorer Devices List Follow"),
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
        REWARD_ISSUES_ERROR("Reward Issues Error"),
        NOTIFICATIONS("notifications"),
        ON("on"),
        OFF("off"),
        USER_RESEARCH_PANEL("User Research Panel"),
        WEB_DOCUMENTATION("Web Documentation"),
        INFO_DAILY_REWARDS("info_daily_rewards"),
        INFO_QOD("info_qod"),
        INFO_POL("info_pol"),
        INFO_CELL_POSITION("info_cell_position"),
        INFO_CELL_CAPACITY("info_cell_capacity"),
        STATION_OFFLINE("station_offline"),
        STATION_DETAILS_CHIP("Station Details Chip"),
        OTA_UPDATE_ID("ota_update"),
        LOW_BATTERY_ID("low_battery"),
        STATION_REGION_ID("station_region"),
        REGION("Region"),
        WARNINGS("Warnings"),
        HOURLY_DETAILS_CARD("Hourly Details Card"),
        HOURLY_FORECAST("hourly_forecast"),
        DAILY_CARD("Daily Card"),
        DAILY_FORECAST("daily_forecast"),
        DAILY_DETAILS("daily_details"),
        NETWORK_STATS("Network Stats"),
        TOKEN_CONTRACT("token_contract"),
        REWARD_CONTRACT("reward_contract"),
        LAST_RUN_HASH("last_run_hash"),
        TOTAL_SUPPLY("total_supply"),
        CIRCULATING_SUPPLY("circulating_supply"),
        TOKEN_CLAIMING_RESULT("Token Claiming Result"),
        REWARD_SPLIT_PRESSED("Reward Split pressed"),
        STAKEHOLDER("Stakeholder"),
        REWARD_SPLITTING_DAILY_REWARD("Reward Splitting In Daily Reward"),
        REWARD_SPLITTING_DEVICE_SETTINGS("Reward Splitting In Device Settings"),
        REWARD_SPLITTING("reward_splitting"),
        NO_REWARD_SPLITTING("no_reward_splitting"),
        STAKEHOLDER_LOWERCASE("stakeholder"),
        NON_STAKEHOLDER("non_stakeholder"),
        FORECAST_NEXT_7_DAYS("forecast_next_7_days"),
        TOKENS_EARNED_PRESSED("Tokens Earned pressed"),
        DEVICE_REWARDS_CARD("Device Rewards Card"),
        OPEN("open"),
        TERMS_OF_USE("Terms Of Use"),
        PRIVACY_POLICY("Privacy Policy"),
        CLOSED("closed"),
        ADD_STATION_PHOTO("Add Station Photo"),
        EXIT_PHOTO_VERIFICATION("Exit Photo Verification"),
        CANCEL_UPLOADING_PHOTOS("Cancel Uploading Photos"),
        RETRY_UPLOADING_PHOTOS("Retry Uploading Photos"),
        START_UPLOADING_PHOTOS("Start Uploading Photos"),
        UPLOADING_PHOTOS_SUCCESS("Uploading Photos Success"),
        GO_TO_PHOTO_VERIFICATION("Go To Photo Verification"),
        CLAIMING_ID("claiming"),
        ANNOUNCEMENT_BUTTON("Announcement Button"),
        STARTED("started"),
        COMPLETED("completed")
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
        STATIONS_OWN("STATIONS_OWN"),
        HAS_WALLET("HAS_WALLET")
    }

    fun setUserId(userId: String)
    fun setUserProperties(params: List<Pair<String, String>>)
    fun setAnalyticsEnabled(enabled: Boolean)
    fun onLogout()
    fun trackScreen(screen: Screen, screenClass: String, itemId: String? = null)
    fun trackEventUserAction(
        actionName: String,
        contentType: String? = null,
        vararg customParams: Pair<String, String>
    )

    fun trackEventViewContent(
        contentName: String,
        vararg customParams: Pair<String, String>,
        success: Long? = null
    )

    fun trackEventPrompt(
        promptName: String,
        promptType: String,
        action: String,
        vararg customParams: Pair<String, String>
    )

    fun trackEventSelectContent(
        contentType: String,
        vararg customParams: Pair<String, String>,
        index: Long? = null
    )
}
