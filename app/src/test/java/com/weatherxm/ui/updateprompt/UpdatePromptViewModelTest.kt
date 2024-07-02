package com.weatherxm.ui.updateprompt

import com.weatherxm.data.repository.AppConfigRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class UpdatePromptViewModelTest : BehaviorSpec({
    val repo = mockk<AppConfigRepository>()
    val viewModel = UpdatePromptViewModel(repo)

    context("Get update-related information") {
        given("Whether the update is mandatory or not") {
            When("Update is mandatory") {
                every { repo.isUpdateMandatory() } returns true
                then("isUpdateMandatory returns true") {
                    viewModel.isUpdateMandatory() shouldBe true
                    verify(exactly = 1) { repo.isUpdateMandatory() }
                }
            }
            When("Update is not mandatory") {
                every { repo.isUpdateMandatory() } returns false
                then("isUpdateMandatory returns false") {
                    viewModel.isUpdateMandatory() shouldBe false
                    verify(exactly = 2) { repo.isUpdateMandatory() }
                }
            }
        }
        given("Update Changelog") {
            every { repo.getChangelog() } returns "Changelog"
            then("getChangelog returns the correct string") {
                viewModel.getChangelog() shouldBe "Changelog"
                verify(exactly = 1) { repo.getChangelog() }
            }
        }
    }
})
