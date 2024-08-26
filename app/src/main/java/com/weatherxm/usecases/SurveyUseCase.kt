package com.weatherxm.usecases

import com.weatherxm.data.Survey
import com.weatherxm.data.repository.SurveyRepository

interface SurveyUseCase {
    fun getSurvey(): Survey?
    fun dismissSurvey(surveyId: String)
}

class SurveyUseCaseImpl(
    private val repo: SurveyRepository
) : SurveyUseCase {
    override fun getSurvey(): Survey? {
        return repo.getSurvey()
    }

    override fun dismissSurvey(surveyId: String) {
        repo.dismissSurvey(surveyId)
    }
}
