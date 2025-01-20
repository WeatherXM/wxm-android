package com.weatherxm.usecases

import com.weatherxm.data.repository.AppConfigRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class UpdatePromptUseCaseTest : BehaviorSpec({
    val repo = mockk<AppConfigRepository>()
    val usecase = UpdatePromptUseCaseImpl(repo)
    val testChangelog = "Changelog"

    fun mockUpdateMandatory(expected: Boolean) =
        every { repo.isUpdateMandatory() } returns expected

    beforeSpec {
        every { repo.getChangelog() } returns testChangelog
    }

    context("Get update-related information") {
        given("Whether the update is mandatory or not") {
            When("Update is mandatory") {
                mockUpdateMandatory(true)
                then("isUpdateMandatory returns true") {
                    usecase.isUpdateMandatory() shouldBe true
                    verify(exactly = 1) { repo.isUpdateMandatory() }
                }
            }
            When("Update is not mandatory") {
                mockUpdateMandatory(false)
                then("isUpdateMandatory returns false") {
                    usecase.isUpdateMandatory() shouldBe false
                    verify(exactly = 2) { repo.isUpdateMandatory() }
                }
            }
        }
        given("Update Changelog") {
            then("getChangelog returns the correct string") {
                usecase.getChangelog() shouldBe testChangelog
                verify(exactly = 1) { repo.getChangelog() }
            }
        }
    }
})
