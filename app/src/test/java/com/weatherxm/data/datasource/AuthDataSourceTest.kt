package com.weatherxm.data.datasource

import com.haroldadmin.cnradapter.NetworkResponse
import com.weatherxm.TestConfig.successUnitResponse
import com.weatherxm.TestUtils.retrofitResponse
import com.weatherxm.TestUtils.testGetFromCache
import com.weatherxm.TestUtils.testNetworkCall
import com.weatherxm.TestUtils.testThrowNotImplemented
import com.weatherxm.data.network.AuthService
import com.weatherxm.data.network.AuthToken
import com.weatherxm.data.network.ErrorResponse
import com.weatherxm.data.network.LoginBody
import com.weatherxm.data.network.LogoutBody
import com.weatherxm.data.network.RefreshBody
import com.weatherxm.data.network.RegistrationBody
import com.weatherxm.data.network.ResetPasswordBody
import com.weatherxm.data.services.CacheService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.coJustRun
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class AuthDataSourceTest : BehaviorSpec({
    val authService = mockk<AuthService>()
    val cacheService = mockk<CacheService>()
    val networkSource = NetworkAuthDataSource(authService)
    val cacheSource = CacheAuthDataSource(cacheService)

    val authToken = mockk<AuthToken>()
    val email = "email"
    val password = "password"
    val token = "token"
    val resetPasswordBody = ResetPasswordBody(email)
    val loginBody = LoginBody(email, password)
    val registrationBody = RegistrationBody(email, null, null)
    val logoutBody = LogoutBody(token, null)
    val refreshBody = RefreshBody(token)
    val successAuthTokenResponse =
        NetworkResponse.Success<AuthToken, ErrorResponse>(authToken, retrofitResponse(authToken))

    beforeSpec {
        coJustRun { cacheService.setAuthToken(authToken) }
        every { authToken.refresh } returns token
    }

    context("Get the Auth Token") {
        When("Using the Cache Source") {
            testGetFromCache(
                "Auth Token",
                authToken,
                mockFunction = { cacheService.getAuthToken() },
                runFunction = { cacheSource.getAuthToken() }
            )
        }
        When("Using the Network Source") {
            testThrowNotImplemented { networkSource.getAuthToken() }
        }
    }

    context("Set the Auth Token") {
        When("Using the Cache Source") {
            then("Set the Auth Token in the Cache") {
                cacheSource.setAuthToken(authToken)
                verify(exactly = 1) { cacheService.setAuthToken(authToken) }
            }
        }
        When("Using the Network Source") {
            then("Should throw a NotImplementedError") {
                shouldThrow<NotImplementedError> { networkSource.setAuthToken(authToken) }
            }
        }
    }

    context("Reset the Password") {
        When("Using the Cache Source") {
            testThrowNotImplemented { cacheSource.resetPassword(email) }
        }
        When("Using the Network Source") {
            testNetworkCall(
                "Unit",
                Unit,
                successUnitResponse,
                mockFunction = { authService.resetPassword(resetPasswordBody) },
                runFunction = { networkSource.resetPassword(email) }
            )
        }
    }

    context("Login the user") {
        When("Using the Cache Source") {
            testThrowNotImplemented { cacheSource.login(email, password) }
        }
        When("Using the Network Source") {
            testNetworkCall(
                "Unit",
                authToken,
                successAuthTokenResponse,
                mockFunction = { authService.login(loginBody) },
                runFunction = { networkSource.login(email, password) }
            )
        }
    }

    context("Signup the user") {
        When("Using the Cache Source") {
            testThrowNotImplemented { cacheSource.signup(email, null, null) }
        }
        When("Using the Network Source") {
            testNetworkCall(
                "Unit",
                Unit,
                successUnitResponse,
                mockFunction = { authService.register(registrationBody) },
                runFunction = { networkSource.signup(email, null, null) }
            )
        }
    }

    context("Logout from the session") {
        When("Using the Cache Source") {
            testThrowNotImplemented { cacheSource.logout(token, null) }
        }
        When("Using the Network Source") {
            testNetworkCall(
                "Unit",
                Unit,
                successUnitResponse,
                mockFunction = { authService.logout(logoutBody) },
                runFunction = { networkSource.logout(token, null) }
            )
        }
    }

    context("Refresh the session") {
        When("Using the Cache Source") {
            testThrowNotImplemented { cacheSource.refresh(authToken) }
        }
        When("Using the Network Source") {
            testNetworkCall(
                "Auth Token",
                authToken,
                successAuthTokenResponse,
                mockFunction = { authService.refresh(refreshBody) },
                runFunction = { networkSource.refresh(authToken) }
            )
        }
    }
})
