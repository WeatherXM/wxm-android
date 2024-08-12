package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.TestConfig.failure
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isError
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.data.repository.FollowRepository
import com.weatherxm.util.WidgetHelper
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify

class FollowUseCaseTest : BehaviorSpec({
    val repo = mockk<FollowRepository>()
    val widgetHelper = mockk<WidgetHelper>()
    val usecase = FollowUseCaseImpl(repo, widgetHelper)

    val validId = "testId"
    val emptyId = ""

    beforeSpec {
        coMockEitherLeft({ repo.followStation(emptyId) }, failure)
        coMockEitherRight({ repo.followStation(validId) }, Unit)
        coMockEitherLeft({ repo.unfollowStation(emptyId) }, failure)
        coMockEitherRight({ repo.unfollowStation(validId) }, Unit)
        justRun { widgetHelper.onUnfollowEvent(validId) }
    }

    context("Follow request in usecase") {
        given("a device ID") {
            When("it's invalid") {
                then("return a Failure") {
                    usecase.followStation(emptyId).isError()
                    coVerify(exactly = 1) { repo.followStation(emptyId) }
                }
            }
            When("it's valid") {
                then("return a success") {
                    usecase.followStation(validId).isSuccess(Unit)
                    coVerify(exactly = 1) { repo.followStation(validId) }
                }
            }
        }
    }

    context("Unfollow request in usecase") {
        given("a device ID") {
            When("it's invalid") {
                then("return a Failure") {
                    usecase.unfollowStation(emptyId).isError()
                    coVerify(exactly = 1) { repo.unfollowStation(emptyId) }
                }
            }
            When("it's valid") {
                then("return a success") {
                    usecase.unfollowStation(validId).isSuccess(Unit)
                    coVerify(exactly = 1) { repo.unfollowStation(validId) }
                }
                and("call the respective function in widgetHelper to reset any widgets") {
                    verify(exactly = 1) { widgetHelper.onUnfollowEvent(validId) }
                }
            }
        }
    }
})
