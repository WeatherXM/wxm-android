package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.TestUtils.mockEitherLeft
import com.weatherxm.data.Failure
import com.weatherxm.data.repository.FollowRepository
import com.weatherxm.util.WidgetHelper
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify

class FollowUseCaseTest : BehaviorSpec({
    val repo = mockk<FollowRepository>()
    val widgetHelper = mockk<WidgetHelper>()
    val usecase = FollowUseCaseImpl(repo, widgetHelper)

    val failure = mockk<Failure>()
    val validId = "testId"
    val emptyId = ""

    beforeSpec {
        mockEitherLeft({ repo.followStation(emptyId) }, failure)
        coMockEitherRight({ repo.followStation(validId) }, Unit)
        mockEitherLeft({ repo.unfollowStation(emptyId) }, failure)
        coMockEitherRight({ repo.unfollowStation(validId) }, Unit)
        every { widgetHelper.onUnfollowEvent(validId) } just Runs
    }

    context("Follow request in usecase") {
        given("a device ID") {
            When("it's invalid") {
                then("return a Failure") {
                    usecase.followStation(emptyId) shouldBe Either.Left(failure)
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
                    usecase.unfollowStation(emptyId) shouldBe Either.Left(failure)
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
