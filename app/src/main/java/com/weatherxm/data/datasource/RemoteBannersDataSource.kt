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

        return if (!showSurvey || id.isEmpty() || lastDismissedId == id) {
            null
        } else {
            Survey(
                id = id,
                title = firebaseRemoteConfig.getString(SURVEY_TITLE),
                message = firebaseRemoteConfig.getString(SURVEY_MESSAGE),
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

        return if (!showBanner || id.isEmpty() || lastDismissedId == id) {
            null
        } else {
            RemoteBanner(
                id = id,
                title = firebaseRemoteConfig.getString(titleKey),
                message = firebaseRemoteConfig.getString(messageKey),
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
