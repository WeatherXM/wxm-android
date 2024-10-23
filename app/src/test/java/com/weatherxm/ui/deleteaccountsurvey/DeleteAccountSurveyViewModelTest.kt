package com.weatherxm.ui.deleteaccountsurvey

import com.weatherxm.data.ClientIdentificationHelper
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.deleteaccountsurvey.DeleteAccountSurveyViewModel.Companion.APP_ID_ENTRY
import com.weatherxm.ui.deleteaccountsurvey.DeleteAccountSurveyViewModel.Companion.USER_ID_ENTRY
import com.weatherxm.usecases.UserUseCase
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class DeleteAccountSurveyViewModelTest : BehaviorSpec({
    val usecase = mockk<UserUseCase>()
    val clientIdentificationHelper = mockk<ClientIdentificationHelper>()
    val viewModel = DeleteAccountSurveyViewModel(usecase, clientIdentificationHelper)

    val userId = "userId"
    val identifier = "identifier"
    val javascriptInjectionCode = "javascript:(function() { " +
        "document.getElementsByClassName('Dq4amc')[0].style.display='none'; " +
        "document.getElementsByClassName('Qr7Oae')[2].style.display='none'; " +
        "document.getElementsByClassName('Qr7Oae')[3].style.display='none'; " +
        "})()"

    val feedbackUrl = "https://www.weatherxm.com/feedback"
    val prefilledFeedbackUrl = "https://www.weatherxm.com/feedback&$APP_ID_ENTRY=$identifier"
    val prefilledFeedbackUrlWithUserId =
        "https://www.weatherxm.com/feedback&$APP_ID_ENTRY=$identifier&$USER_ID_ENTRY=$userId"

    val nonResponseUrl = "https://www.weatherxm.com"
    val responseUrl = "https://www.weatherxm.com/formResponse"


    beforeSpec {
        every { clientIdentificationHelper.getInterceptorClientIdentifier() } returns identifier
    }

    context("Get JavaScript Injection code") {
        given("The function that returns it") {
            then("Use this function to return it") {
                viewModel.getJavascriptInjectionCode() shouldBe javascriptInjectionCode
            }
        }
    }

    context("Get if a URL is the response in the form") {
        given("a url") {
            When("the URL is null") {
                then("return false") {
                    viewModel.isUrlFormResponse(null) shouldBe false
                }
            }
            When("the URL is empty") {
                then("return false") {
                    viewModel.isUrlFormResponse("") shouldBe false
                }
            }
            When("the URL is not the response in the form") {
                then("return false") {
                    viewModel.isUrlFormResponse(nonResponseUrl) shouldBe false
                }
            }
            When("the URL is the response in the form") {
                then("return true") {
                    viewModel.isUrlFormResponse(responseUrl) shouldBe true
                }
            }
        }
    }

    context("Prefill the feedback URL and return it") {
        given("a feedback URL") {
            and("a user ID") {
                When("the user ID is empty") {
                    every { usecase.getUserId() } returns String.empty()
                    then("prefill the URL without the user ID") {
                        viewModel.getPrefilledSurveyFormUrl(
                            feedbackUrl
                        ) shouldBe prefilledFeedbackUrl
                    }
                }
                When("the user ID is not empty") {
                    every { usecase.getUserId() } returns userId
                    then("prefill the URL with the user ID") {
                        viewModel.getPrefilledSurveyFormUrl(
                            feedbackUrl
                        ) shouldBe prefilledFeedbackUrlWithUserId
                    }
                }
            }
        }
    }
})
