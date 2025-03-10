package com.weatherxm.ui.updateprompt

import com.weatherxm.usecases.UpdatePromptUseCase
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class UpdatePromptViewModelTest : BehaviorSpec({
    val usecase = mockk<UpdatePromptUseCase>()
    val viewModel = UpdatePromptViewModel(usecase)
    val testChangelog = "Changelog"

    fun mockUpdateMandatory(expected: Boolean) =
        every { usecase.isUpdateMandatory() } returns expected

    beforeSpec {
        every { usecase.getChangelog() } returns testChangelog
    }

    context("Get update-related information") {
        given("Whether the update is mandatory or not") {
            When("Update is mandatory") {
                mockUpdateMandatory(true)
                then("isUpdateMandatory returns true") {
                    viewModel.isUpdateMandatory() shouldBe true
                    verify(exactly = 1) { usecase.isUpdateMandatory() }
                }
            }
            When("Update is not mandatory") {
                mockUpdateMandatory(false)
                then("isUpdateMandatory returns false") {
                    viewModel.isUpdateMandatory() shouldBe false
                    verify(exactly = 2) { usecase.isUpdateMandatory() }
                }
            }
        }
        given("Update Changelog") {
            then("getChangelog returns the correct string") {
                viewModel.getChangelog() shouldBe testChangelog
                verify(exactly = 1) { usecase.getChangelog() }
            }
        }
    }
})
