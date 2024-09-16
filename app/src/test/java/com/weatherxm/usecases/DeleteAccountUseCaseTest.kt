package com.weatherxm.usecases

import com.weatherxm.TestConfig.failure
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isError
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.data.repository.AuthRepository
import com.weatherxm.data.repository.UserRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk

class DeleteAccountUseCaseTest : BehaviorSpec({
    val userRepository = mockk<UserRepository>()
    val authRepository = mockk<AuthRepository>()
    val usecase = DeleteAccountUseCaseImpl(userRepository, authRepository)

    val password = "password"

    beforeSpec {
        coJustRun { authRepository.logout() }
    }

    context("Get if a password is correct") {
        given("A repository providing the answer") {
            When("it's a success") {
                coMockEitherRight({ authRepository.isPasswordCorrect(password) }, true)
                then("return that answer") {
                    usecase.isPasswordCorrect(password).isSuccess(true)
                }
            }
            When("it's a failure") {
                coMockEitherLeft({ authRepository.isPasswordCorrect(password) }, failure)
                then("return that failure") {
                    usecase.isPasswordCorrect(password).isError()
                }
            }
        }
    }

    context("Delete an account") {
        given("A repository providing the delete functionality") {
            When("it's a success") {
                coMockEitherRight({ userRepository.deleteAccount() }, Unit)
                then("return the success") {
                    usecase.deleteAccount().isSuccess(Unit)
                }
                then("logout the user") {
                    coVerify(exactly = 1) { authRepository.logout() }
                }
            }
            When("it's a failure") {
                coMockEitherLeft({ userRepository.deleteAccount() }, failure)
                then("return that failure") {
                    usecase.deleteAccount().isError()
                }
            }
        }
    }
})
