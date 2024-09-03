package com.weatherxm.ui.home.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.Resource
import com.weatherxm.data.SingleLiveEvent
import com.weatherxm.data.Survey
import com.weatherxm.data.User
import com.weatherxm.usecases.SurveyUseCase
import com.weatherxm.usecases.UserUseCase
import com.weatherxm.util.Failure.getDefaultMessage
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val useCase: UserUseCase,
    private val surveyUseCase: SurveyUseCase,
    private val analytics: AnalyticsWrapper
) : ViewModel() {
    private val onUser = MutableLiveData<Resource<User>>()
    private val onSurvey = SingleLiveEvent<Survey>()

    fun onUser(): LiveData<Resource<User>> = onUser
    fun onSurvey(): LiveData<Survey> = onSurvey

    fun fetchUser(forceRefresh: Boolean = false) {
        onUser.postValue(Resource.loading())
        viewModelScope.launch {
            useCase.getUser(forceRefresh)
                .onRight {
                    onUser.postValue(Resource.success(it))
                }
                .onLeft {
                    analytics.trackEventFailure(it.code)
                    onUser.postValue(
                        Resource.error(it.getDefaultMessage(R.string.error_reach_out_short))
                    )
                }
        }
    }

    fun getSurvey() {
        surveyUseCase.getSurvey().apply {
            if (this != null) {
                onSurvey.postValue(this)
            }
        }
    }

    fun dismissSurvey(surveyId: String) {
        surveyUseCase.dismissSurvey(surveyId)
    }
}
