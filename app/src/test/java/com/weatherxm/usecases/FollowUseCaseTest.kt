package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.TestUtils.isDeviceNotFound
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.data.ApiError
import com.weatherxm.data.repository.FollowRepository
import com.weatherxm.util.WidgetHelper
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify

class FollowUseCaseTest : BehaviorSpec({
    val repo = mockk<FollowRepository>()
    val widgetHelper = mockk<WidgetHelper>()
    val usecase = FollowUseCaseImpl(repo, widgetHelper)

    val validId = "testId"
    val emptyId = ""

    beforeSpec {
        coEvery { repo.followStation(emptyId) } returns Either.Left(ApiError.DeviceNotFound(""))
        coEvery { repo.followStation(validId) } returns Either.Right(Unit)
        coEvery { repo.unfollowStation(emptyId) } returns Either.Left(ApiError.DeviceNotFound(""))
        coEvery { repo.unfollowStation(validId) } returns Either.Right(Unit)
        every { widgetHelper.onUnfollowEvent(validId) } just Runs
    }

    context("Follow request in usecase") {
        given("a device ID") {
            When("it's invalid") {
                then("return a Failure") {
                    usecase.followStation(emptyId).isLeft { it.isDeviceNotFound() } shouldBe true
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
                    usecase.unfollowStation(emptyId).isLeft { it.isDeviceNotFound() } shouldBe true
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
