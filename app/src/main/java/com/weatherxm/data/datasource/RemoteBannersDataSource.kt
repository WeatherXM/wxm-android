package com.weatherxm.data.datasource

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.weatherxm.data.models.RemoteBanner
import com.weatherxm.data.models.RemoteBannerType
import com.weatherxm.data.models.Survey
import com.weatherxm.data.services.CacheService

interface RemoteBannersDataSource {
    fun getSurvey(): Survey?
    fun dismissSurvey(surveyId: String)
    fun getRemoteBanner(bannerType: RemoteBannerType): RemoteBanner?
    fun dismissRemoteBanner(bannerType: RemoteBannerType, bannerId: String)
}

class RemoteBannersDataSourceImpl(
    private val firebaseRemoteConfig: FirebaseRemoteConfig,
    private val cacheService: CacheService
) : RemoteBannersDataSource {

    companion object {
        const val SURVEY_ID = "survey_id"
        const val SURVEY_TITLE = "survey_title"
        const val SURVEY_MESSAGE = "survey_message"
        const val SURVEY_URL = "survey_url"
        const val SURVEY_ACTION_LABEL = "survey_action_label"
        const val SURVEY_SHOW = "survey_show"
        const val INFO_BANNER_ID = "info_banner_id"
        const val INFO_BANNER_TITLE = "info_banner_title"
        const val INFO_BANNER_MESSAGE = "info_banner_message"
        const val INFO_BANNER_ACTION_URL = "info_banner_action_url"
        const val INFO_BANNER_ACTION_LABEL = "info_banner_action_label"
        const val INFO_BANNER_SHOW = "info_banner_show"
        const val INFO_BANNER_DISMISSABLE = "info_banner_dismissable"
        const val INFO_BANNER_ACTION_SHOW = "info_banner_action_show"
        const val ANNOUNCEMENT_ID = "announcement_id"
        const val ANNOUNCEMENT_TITLE = "announcement_title"
        const val ANNOUNCEMENT_MESSAGE = "announcement_message"
        const val ANNOUNCEMENT_ACTION_URL = "announcement_action_url"
        const val ANNOUNCEMENT_ACTION_LABEL = "announcement_action_label"
        const val ANNOUNCEMENT_ACTION_SHOW = "announcement_action_show"
        const val ANNOUNCEMENT_SHOW = "announcement_show"
        const val ANNOUNCEMENT_DISMISSABLE = "announcement_dismissable"
        const val ANNOUNCEMENT_LOCAL_PRO_ACTION_URL = "weatherxm://announcement/weatherxm_pro"
    }

    override fun getSurvey(): Survey? {
        val id = firebaseRemoteConfig.getString(SURVEY_ID)
        val showSurvey = firebaseRemoteConfig.getBoolean(SURVEY_SHOW)
        val lastDismissedId = cacheService.getLastDismissedSurveyId()

        /**
         * STOPSHIP: Change the below code - for testing purposes only.
         * 1. if
         * 2. message
         */
        return if (false) {
            null
        } else {
            Survey(
                id = id,
                title = firebaseRemoteConfig.getString(SURVEY_TITLE),
                message = "Alright, let's break down what's happening with your \"Exotic pecan aurora\" M5 weather station in Chania, GR.\n\n**Good News:**\n\n*   Your station is **active** and has earned a total of **842.75 WXM**!\n*   The current weather data is being reported, so the station is generally online.\n\n**Identified Issues:**\n\n1.  **Minor Humidity Sensor Issues:** The data quality checks are detecting occasional inaccuracies with your humidity sensor. This isn't a critical problem, but it's something to keep an eye on.\n    *   **Solution:** This can happen from time to time. You can find more information on sensor issues [here](https://docs.weatherxm.com/rewards/rewards-troubleshooting#sensor-problems). If the problem persists, consider the troubleshooting steps below.\n2.  **Data Gaps:** The station is experiencing significant data gaps, which are causing you to lose rewards. This indicates a connectivity problem between the indoor (WG1000) and outdoor (WS1000) units, or with the network.\n\n**Troubleshooting Steps for Data Gaps:**\n\nSince you have the M5 bundle, here's what you should check:\n\n*   **WG1000 Gateway:**\n    *   **RF Connection:** Make sure the connection between the WS1000 and WG1000 is stable. Refer to this guide: [Weather Station Not Reporting Data](https://docs.weatherxm.com/wxm-devices/m5/troubleshooting#weather-station-not-reporting-data-to-wg1000-gateway-there-are-dashes----on-the-display-console).\n    *   **External Antenna:** Consider adding the included external antenna to improve the connection: [Adding External Antenna](https://docs.weatherxm.com/wxm-devices/m5/troubleshooting#my-station-does-not-connect-to-the-wg1000-gateway).\n    *   **Battery Level:** Check the battery level of the WS1000 via the mobile app. Low battery can cause unstable connections.\n        ![Image](https://docs.weatherxm.com//img/rewards/settings-through-the-app.png)\n        ![Image](https://docs.weatherxm.com//img/rewards/battery-level.png)\n    *   **GPS:** Ensure the WG1000 has a good GPS signal: [GPS Troubleshooting](https://docs.weatherxm.com/wxm-devices/m5/troubleshooting#no-location-gps-data-arrow-icon-is-red).\n    *   **Wi-Fi:** Verify the WG1000 has a stable Wi-Fi connection: [Wi-Fi Troubleshooting](https://docs.weatherxm.com/wxm-devices/m5/troubleshooting#no-wifi--connection).\n    *   **WG1000 Gateway Icons:** Here's a reminder of what the icons on your WG1000 mean:\n        ![Image](https://docs.weatherxm.com//img/rewards/m5-ui-new-numbered.jpg)\n\n**Troubleshooting Steps for Humidity Sensor:**\n\n*   **Check Installation:** Ensure the WS1000 is properly installed according to the [Best Practices](https://docs.weatherxm.com/wxm-devices/m5/install-weather-station#best-practices-for-proper-station-installation).\n*   **Power Cycle:** If the humidity sensor issues persist, try a power cycle:\n    1.  Remove batteries.\n    2.  Cover the solar panel.\n    3.  Wait 48 hours.\n    4.  Reinsert batteries.\n\nIf you've gone through these steps and are still having trouble, please submit a support ticket at [WeatherXM Support](https://help.weatherxm.com) for further assistance!\n",
                url = firebaseRemoteConfig.getString(SURVEY_URL),
                actionLabel = firebaseRemoteConfig.getString(SURVEY_ACTION_LABEL)
            )
        }
    }

    override fun dismissSurvey(surveyId: String) {
        cacheService.setLastDismissedSurveyId(surveyId)
    }

    override fun getRemoteBanner(bannerType: RemoteBannerType): RemoteBanner? {
        var idKey: String
        var titleKey: String
        var messageKey: String
        var actionUrlKey: String
        var actionLabelKey: String
        var actionShowKey: String
        var showKey: String
        var dismissableKey: String

        when (bannerType) {
            RemoteBannerType.INFO_BANNER -> {
                idKey = INFO_BANNER_ID
                titleKey = INFO_BANNER_TITLE
                messageKey = INFO_BANNER_MESSAGE
                actionUrlKey = INFO_BANNER_ACTION_URL
                actionLabelKey = INFO_BANNER_ACTION_LABEL
                actionShowKey = INFO_BANNER_ACTION_SHOW
                showKey = INFO_BANNER_SHOW
                dismissableKey = INFO_BANNER_DISMISSABLE
            }
            RemoteBannerType.ANNOUNCEMENT -> {
                idKey = ANNOUNCEMENT_ID
                titleKey = ANNOUNCEMENT_TITLE
                messageKey = ANNOUNCEMENT_MESSAGE
                actionUrlKey = ANNOUNCEMENT_ACTION_URL
                actionLabelKey = ANNOUNCEMENT_ACTION_LABEL
                actionShowKey = ANNOUNCEMENT_ACTION_SHOW
                showKey = ANNOUNCEMENT_SHOW
                dismissableKey = ANNOUNCEMENT_DISMISSABLE
            }
        }

        val id = firebaseRemoteConfig.getString(idKey)
        val showBanner = firebaseRemoteConfig.getBoolean(showKey)
        val lastDismissedId = cacheService.getLastDismissedRemoteBannerId(bannerType)

        /**
         * STOPSHIP: Change the below code - for testing purposes only.
         * 1. if
         * 2. message
         */
        return if (false) {
            null
        } else {
            RemoteBanner(
                id = id,
                title = firebaseRemoteConfig.getString(titleKey),
                message = "Alright, let's break down what's happening with your \"Exotic pecan aurora\" M5 weather station in Chania, GR.\n\n**Good News:**\n\n*   Your station is **active** and has earned a total of **842.75 WXM**!\n*   The current weather data is being reported, so the station is generally online.\n\n**Identified Issues:**\n\n1.  **Minor Humidity Sensor Issues:** The data quality checks are detecting occasional inaccuracies with your humidity sensor. This isn't a critical problem, but it's something to keep an eye on.\n    *   **Solution:** This can happen from time to time. You can find more information on sensor issues [here](https://docs.weatherxm.com/rewards/rewards-troubleshooting#sensor-problems). If the problem persists, consider the troubleshooting steps below.\n2.  **Data Gaps:** The station is experiencing significant data gaps, which are causing you to lose rewards. This indicates a connectivity problem between the indoor (WG1000) and outdoor (WS1000) units, or with the network.\n\n**Troubleshooting Steps for Data Gaps:**\n\nSince you have the M5 bundle, here's what you should check:\n\n*   **WG1000 Gateway:**\n    *   **RF Connection:** Make sure the connection between the WS1000 and WG1000 is stable. Refer to this guide: [Weather Station Not Reporting Data](https://docs.weatherxm.com/wxm-devices/m5/troubleshooting#weather-station-not-reporting-data-to-wg1000-gateway-there-are-dashes----on-the-display-console).\n    *   **External Antenna:** Consider adding the included external antenna to improve the connection: [Adding External Antenna](https://docs.weatherxm.com/wxm-devices/m5/troubleshooting#my-station-does-not-connect-to-the-wg1000-gateway).\n    *   **Battery Level:** Check the battery level of the WS1000 via the mobile app. Low battery can cause unstable connections.\n        ![Image](https://docs.weatherxm.com//img/rewards/settings-through-the-app.png)\n        ![Image](https://docs.weatherxm.com//img/rewards/battery-level.png)\n    *   **GPS:** Ensure the WG1000 has a good GPS signal: [GPS Troubleshooting](https://docs.weatherxm.com/wxm-devices/m5/troubleshooting#no-location-gps-data-arrow-icon-is-red).\n    *   **Wi-Fi:** Verify the WG1000 has a stable Wi-Fi connection: [Wi-Fi Troubleshooting](https://docs.weatherxm.com/wxm-devices/m5/troubleshooting#no-wifi--connection).\n    *   **WG1000 Gateway Icons:** Here's a reminder of what the icons on your WG1000 mean:\n        ![Image](https://docs.weatherxm.com//img/rewards/m5-ui-new-numbered.jpg)\n\n**Troubleshooting Steps for Humidity Sensor:**\n\n*   **Check Installation:** Ensure the WS1000 is properly installed according to the [Best Practices](https://docs.weatherxm.com/wxm-devices/m5/install-weather-station#best-practices-for-proper-station-installation).\n*   **Power Cycle:** If the humidity sensor issues persist, try a power cycle:\n    1.  Remove batteries.\n    2.  Cover the solar panel.\n    3.  Wait 48 hours.\n    4.  Reinsert batteries.\n\nIf you've gone through these steps and are still having trouble, please submit a support ticket at [WeatherXM Support](https://help.weatherxm.com) for further assistance!\n",
                url = firebaseRemoteConfig.getString(actionUrlKey),
                actionLabel = firebaseRemoteConfig.getString(actionLabelKey),
                showActionButton = firebaseRemoteConfig.getBoolean(actionShowKey),
                showCloseButton = firebaseRemoteConfig.getBoolean(dismissableKey)
            )
        }
    }

    override fun dismissRemoteBanner(bannerType: RemoteBannerType, bannerId: String) {
        cacheService.setLastDismissedRemoteBannerId(bannerType, bannerId)
    }
}
