package com.weatherxm.data.datasource

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.weatherxm.data.InfoBanner
import com.weatherxm.data.Survey
import com.weatherxm.data.services.CacheService

interface RemoteBannersDataSource {
    fun getSurvey(): Survey?
    fun dismissSurvey(surveyId: String)
    fun getInfoBanner(): InfoBanner?
    fun dismissInfoBanner(infoBannerId: String)
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
        const val INFO_BANNER_URL = "info_banner_url"
        const val INFO_BANNER_ACTION_LABEL = "info_banner_action_label"
        const val INFO_BANNER_SHOW = "info_banner_show"
        const val INFO_BANNER_DISMISSABLE = "info_banner_dismissable"
        const val INFO_BUTTON_SHOW = "info_button_show"
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

    override fun getInfoBanner(): InfoBanner? {
        val id = firebaseRemoteConfig.getString(INFO_BANNER_ID)
        val showSurvey = firebaseRemoteConfig.getBoolean(INFO_BANNER_SHOW)
        val lastDismissedId = cacheService.getLastDismissedInfoBannerId()

        return if (!showSurvey || id.isEmpty() || lastDismissedId == id) {
            null
        } else {
            InfoBanner(
                id = id,
                title = firebaseRemoteConfig.getString(INFO_BANNER_TITLE),
                message = firebaseRemoteConfig.getString(INFO_BANNER_MESSAGE),
                url = firebaseRemoteConfig.getString(INFO_BANNER_URL),
                actionLabel = firebaseRemoteConfig.getString(INFO_BANNER_ACTION_LABEL),
                showActionButton = firebaseRemoteConfig.getBoolean(INFO_BUTTON_SHOW),
                showCloseButton = firebaseRemoteConfig.getBoolean(INFO_BANNER_DISMISSABLE)
            )
        }
    }

    override fun dismissInfoBanner(infoBannerId: String) {
        cacheService.setLastDismissedInfoBannerId(infoBannerId)
    }
}
