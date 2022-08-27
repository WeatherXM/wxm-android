package com.weatherxm.ui.sendfeedback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weatherxm.R
import com.weatherxm.usecases.SendFeedbackUseCase
import com.weatherxm.util.ResourcesHelper
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SendFeedbackViewModel : ViewModel(), KoinComponent {
    companion object {
        const val USER_ID_ENTRY = "entry.695293761"
        const val APP_ID_ENTRY = "entry.2052436656"
    }

    private val useCase: SendFeedbackUseCase by inject()
    private val resHelper: ResourcesHelper by inject()

    // Needed for adding user-id info on the google form (used for sending feedback)
    private lateinit var userId: String

    fun getPrefilledFormUrl(): String {
        val clientIdentifier = useCase.getClientIdentifier()
        val feedbackUrl = resHelper.getString(R.string.short_app_survey_url)
        return if (userId.isNotEmpty()) {
            "$feedbackUrl&$APP_ID_ENTRY=$clientIdentifier&$USER_ID_ENTRY=$userId"
        } else {
            "$feedbackUrl&$APP_ID_ENTRY=$clientIdentifier"
        }
    }

    fun isUrlFormResponse(newUrl: String?): Boolean {
        return newUrl?.endsWith("formResponse") ?: false
    }

    init {
        viewModelScope.launch {
            useCase.getUser()
                .map {
                    userId = it.id
                }
                .mapLeft {
                    userId = ""
                }
        }
    }
}
