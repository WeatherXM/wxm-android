package com.weatherxm.usecases

import com.weatherxm.data.InfoBanner
import com.weatherxm.data.Survey
import com.weatherxm.data.repository.RemoteBannersRepository

interface RemoteBannersUseCase {
    fun getSurvey(): Survey?
    fun dismissSurvey(surveyId: String)
    fun getInfoBanner(): InfoBanner?
    fun dismissInfoBanner(infoBannerId: String)
}

class RemoteBannersUseCaseImpl(
    private val repo: RemoteBannersRepository
) : RemoteBannersUseCase {
    override fun getSurvey(): Survey? {
        return repo.getSurvey()
    }

    override fun dismissSurvey(surveyId: String) {
        repo.dismissSurvey(surveyId)
    }

    override fun getInfoBanner(): InfoBanner? {
        return repo.getInfoBanner()
    }

    override fun dismissInfoBanner(infoBannerId: String) {
        repo.dismissInfoBanner(infoBannerId)
    }
}
