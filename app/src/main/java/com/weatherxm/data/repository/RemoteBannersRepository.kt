package com.weatherxm.data.repository

import com.weatherxm.data.InfoBanner
import com.weatherxm.data.Survey
import com.weatherxm.data.datasource.RemoteBannersDataSource

interface RemoteBannersRepository {
    fun getSurvey(): Survey?
    fun dismissSurvey(surveyId: String)
    fun getInfoBanner(): InfoBanner?
    fun dismissInfoBanner(infoBannerId: String)
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

    override fun getInfoBanner(): InfoBanner? {
        return dataSource.getInfoBanner()
    }

    override fun dismissInfoBanner(infoBannerId: String) {
        dataSource.dismissInfoBanner(infoBannerId)
    }
}
