package com.weatherxm.ui.photoverification.intro

import com.weatherxm.usecases.DevicePhotoUseCase
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify

class PhotoVerificationIntroViewModelTest : BehaviorSpec({
    val usecase = mockk<DevicePhotoUseCase>()
    val viewModel = PhotoVerificationIntroViewModel(usecase)

    beforeSpec {
        justRun { usecase.setAcceptedTerms() }
    }

    context("Get / Set if the user has accepted the uploading photos terms") {
        given("The usecase providing the GET / SET mechanisms") {
            When("We should get the user's accepted status") {
                and("user has not accepted the terms") {
                    every { usecase.getAcceptedTerms() } returns false
                    then("return false") {
                        viewModel.getAcceptedTerms() shouldBe false
                    }
                }
                and("user has accepted the terms") {
                    every { usecase.getAcceptedTerms() } returns true
                    then("return true") {
                        viewModel.getAcceptedTerms() shouldBe true
                    }
                }
            }
            When("We should set that the user has accepted the terms") {
                then("ensure that the SET takes place in the usecase") {
                    viewModel.setAcceptedTerms()
                    verify(exactly = 1) { usecase.setAcceptedTerms() }
                }
            }
        }
    }
})
