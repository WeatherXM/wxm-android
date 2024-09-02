package com.weatherxm.data.datasource

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.weatherxm.data.Survey
import com.weatherxm.data.services.CacheService

interface SurveyDataSource {
    fun getSurvey(): Survey?
    fun dismissSurvey(surveyId: String)
}

class SurveyDataSourceImpl(
    private val firebaseRemoteConfig: FirebaseRemoteConfig,
    private val cacheService: CacheService
) : SurveyDataSource {

    companion object {
        const val SURVEY_ID = "survey_id"
        const val SURVEY_TITLE = "survey_title"
        const val SURVEY_MESSAGE = "survey_message"
        const val SURVEY_URL = "survey_url"
        const val SURVEY_ACTION_LABEL = "survey_action_label"
        const val SURVEY_SHOW = "survey_show"
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
}
