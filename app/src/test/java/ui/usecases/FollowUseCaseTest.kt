package ui.usecases

import arrow.core.Either
import com.weatherxm.data.ApiError
import com.weatherxm.data.repository.FollowRepository
import com.weatherxm.usecases.FollowUseCaseImpl
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

    beforeSpec {
        coEvery { repo.followStation("") } returns Either.Left(ApiError.DeviceNotFound(""))
        coEvery { repo.followStation("testId") } returns Either.Right(Unit)
        coEvery { repo.unfollowStation("") } returns Either.Left(ApiError.DeviceNotFound(""))
        coEvery { repo.unfollowStation("testId") } returns Either.Right(Unit)
        every { widgetHelper.onUnfollowEvent("testId") } just Runs
    }

    context("Follow request in usecase") {
        given("a device ID") {
            When("it's invalid") {
                then("return a Failure") {
                    usecase.followStation("")
                        .isLeft { it is ApiError.DeviceNotFound } shouldBe true
                    coVerify(exactly = 1) { repo.followStation("") }
                }
            }
            When("it's valid") {
                then("return a success") {
                    usecase.followStation("testId") shouldBe Either.Right(Unit)
                    coVerify(exactly = 1) { repo.followStation("testId") }
                }
            }
        }
    }

    context("Unfollow request in usecase") {
        given("a device ID") {
            When("it's invalid") {
                then("return a Failure") {
                    usecase.unfollowStation("")
                        .isLeft { it is ApiError.DeviceNotFound } shouldBe true
                    coVerify(exactly = 1) { repo.unfollowStation("") }
                }
            }
            When("it's valid") {
                then("return a success") {
                    usecase.unfollowStation("testId") shouldBe Either.Right(Unit)
                    coVerify(exactly = 1) { repo.unfollowStation("testId") }
                }
                and("call the respective function in widgetHelper to reset any widgets") {
                    verify(exactly = 1) { widgetHelper.onUnfollowEvent("testId") }
                }
            }
        }
    }
})
