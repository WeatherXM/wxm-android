package com.weatherxm.data.repository

import com.weatherxm.data.datasource.RemoteBannersDataSource
import com.weatherxm.data.models.RemoteBanner
import com.weatherxm.data.models.RemoteBannerType
import com.weatherxm.data.models.Survey

interface RemoteBannersRepository {
    fun getSurvey(): Survey?
    fun dismissSurvey(surveyId: String)
    fun getRemoteBanner(bannerType: RemoteBannerType): RemoteBanner?
    fun dismissRemoteBanner(bannerType: RemoteBannerType, bannerId: String)
}

class RemoteBannersRepositoryImpl(
    private val dataSource: RemoteBannersDataSource
) : RemoteBannersRepository {
    override fun getSurvey(): Survey? {
        return dataSource.getSurvey()
    }

    override fun dismissSurvey(surveyId: String) {
        dataSource.dismissSurvey(surveyId)
    }

    override fun getRemoteBanner(bannerType: RemoteBannerType): RemoteBanner? {
        return dataSource.getRemoteBanner(bannerType)
    }

    override fun dismissRemoteBanner(bannerType: RemoteBannerType, bannerId: String) {
        dataSource.dismissRemoteBanner(bannerType, bannerId)
    }
}
