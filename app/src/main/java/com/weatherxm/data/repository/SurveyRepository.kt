package com.weatherxm.data.repository

import com.weatherxm.data.Survey
import com.weatherxm.data.datasource.SurveyDataSource

interface SurveyRepository {
    fun getSurvey(): Survey?
    fun dismissSurvey(surveyId: String)
}

class SurveyRepositoryImpl(
    private val dataSource: SurveyDataSource
) : SurveyRepository {
    override fun getSurvey(): Survey? {
        return dataSource.getSurvey()
    }

    override fun dismissSurvey(surveyId: String) {
        dataSource.dismissSurvey(surveyId)
    }
}
