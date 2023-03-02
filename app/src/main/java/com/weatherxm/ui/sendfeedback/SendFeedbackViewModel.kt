package com.weatherxm.ui.sendfeedback

import androidx.lifecycle.ViewModel
import com.weatherxm.R
import com.weatherxm.data.ClientIdentificationHelper
import com.weatherxm.usecases.SendFeedbackUseCase
import com.weatherxm.util.ResourcesHelper
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SendFeedbackViewModel : ViewModel(), KoinComponent {
    companion object {
        const val USER_ID_ENTRY = "entry.695293761"
        const val APP_ID_ENTRY = "entry.2052436656"
    }

    private val useCase: SendFeedbackUseCase by inject()
    private val clientIdentificationHelper: ClientIdentificationHelper by inject()
    private val resHelper: ResourcesHelper by inject()

    fun getPrefilledSurveyFormUrl(isDeleteAccount: Boolean): String {
        val clientIdentifier = clientIdentificationHelper.getInterceptorClientIdentifier()
        val userId = useCase.getUserId()
        val feedbackUrl = if (isDeleteAccount) {
            resHelper.getString(R.string.delete_account_survey_url)
        } else {
            resHelper.getString(R.string.short_app_survey_url)
        }

        return if (userId.isNotEmpty()) {
            "$feedbackUrl&$APP_ID_ENTRY=$clientIdentifier&$USER_ID_ENTRY=$userId"
        } else {
            "$feedbackUrl&$APP_ID_ENTRY=$clientIdentifier"
        }
    }

    fun getJavascriptInjectionCodeSurveyForm(): String {
        return "javascript:(function() { " +
            "document.getElementsByClassName('Dq4amc')[0].style.display='none'; " +
            "document.getElementsByClassName('Qr7Oae')[5].style.display='none'; " +
            "document.getElementsByClassName('Qr7Oae')[6].style.display='none'; " +
            "})()"
    }

    fun getJavascriptInjectionCodeDeleteForm(): String {
        return "javascript:(function() { " +
            "document.getElementsByClassName('Dq4amc')[0].style.display='none'; " +
            "document.getElementsByClassName('Qr7Oae')[2].style.display='none'; " +
            "document.getElementsByClassName('Qr7Oae')[3].style.display='none'; " +
            "})()"
    }

    fun isUrlFormResponse(newUrl: String?): Boolean {
        return newUrl?.endsWith("formResponse") ?: false
    }
}
