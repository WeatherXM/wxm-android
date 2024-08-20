package com.weatherxm.ui.deleteaccountsurvey

import androidx.lifecycle.ViewModel
import com.weatherxm.data.ClientIdentificationHelper
import com.weatherxm.usecases.DeleteAccountSurveyUseCase

class DeleteAccountSurveyViewModel(
    private val useCase: DeleteAccountSurveyUseCase,
    private val clientIdentificationHelper: ClientIdentificationHelper
) : ViewModel() {
    companion object {
        const val USER_ID_ENTRY = "entry.695293761"
        const val APP_ID_ENTRY = "entry.2052436656"
    }

    fun getPrefilledSurveyFormUrl(feedbackUrl: String): String {
        val clientIdentifier = clientIdentificationHelper.getInterceptorClientIdentifier()
        val userId = useCase.getUserId()

        return if (userId.isNotEmpty()) {
            "$feedbackUrl&$APP_ID_ENTRY=$clientIdentifier&$USER_ID_ENTRY=$userId"
        } else {
            "$feedbackUrl&$APP_ID_ENTRY=$clientIdentifier"
        }
    }

    fun getJavascriptInjectionCode(): String {
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
