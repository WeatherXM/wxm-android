package com.weatherxm.usecases

import com.weatherxm.data.models.RemoteBanner
import com.weatherxm.data.models.RemoteBannerType
import com.weatherxm.data.models.Survey
import com.weatherxm.data.repository.RemoteBannersRepository

interface RemoteBannersUseCase {
    fun getSurvey(): Survey?
    fun dismissSurvey(surveyId: String)
    fun getRemoteBanner(bannerType: RemoteBannerType): RemoteBanner?
    fun dismissRemoteBanner(bannerType: RemoteBannerType, bannerId: String)
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

    override fun getRemoteBanner(bannerType: RemoteBannerType): RemoteBanner? {
        return repo.getRemoteBanner(bannerType)
    }

    override fun dismissRemoteBanner(bannerType: RemoteBannerType, bannerId: String) {
        repo.dismissRemoteBanner(bannerType, bannerId)
    }
}
