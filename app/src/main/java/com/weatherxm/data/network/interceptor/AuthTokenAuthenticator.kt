package com.weatherxm.data.network.interceptor

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.handleErrorWith
import com.weatherxm.data.AuthError
import com.weatherxm.data.AuthError.InvalidAuthTokenError
import com.weatherxm.data.AuthError.InvalidCredentialsError
import com.weatherxm.data.AuthError.InvalidRefreshTokenError
import com.weatherxm.data.Failure
import com.weatherxm.data.datasource.AuthTokenDataSource
import com.weatherxm.data.datasource.CredentialsDataSource
import com.weatherxm.data.map
import com.weatherxm.data.network.AuthService
import com.weatherxm.data.network.AuthToken
import com.weatherxm.data.network.Credentials
import com.weatherxm.data.network.LoginBody
import com.weatherxm.data.network.RefreshBody
import com.weatherxm.data.network.interceptor.ApiRequestInterceptor.Companion.AUTH_HEADER
import com.weatherxm.data.path
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class AuthTokenAuthenticator : Authenticator, KoinComponent {

    private val authService: AuthService by inject()
    private val authTokenRepository: AuthTokenDataSource by inject()
    private val credentialsRepository: CredentialsDataSource by inject()

    override fun authenticate(route: Route?, response: Response): Request? {
        // The original request
        val request = response.request

        Timber.d("[${request.path()}] Status: ${response.code}. Invoking authenticator.")

        return refreshAndRetry(request)
            .handleErrorWith { loginAndRetry(request) }
            .tapLeft { Timber.w("[${request.path()}] Failed to authenticate. Giving up.") }
            .orNull()
    }

    private fun getAuthToken(): Either<AuthError, AuthToken> = runBlocking {
        authTokenRepository.getAuthToken()
            .mapLeft {
                InvalidAuthTokenError
            }
            .flatMap { authToken ->
                if (!authToken.isRefreshTokenValid()) {
                    Either.Left(InvalidRefreshTokenError)
                } else {
                    Either.Right(authToken)
                }
            }
    }

    private fun getCredentials(): Either<AuthError, Credentials> = runBlocking {
        credentialsRepository.getCredentials()
            .mapLeft {
                InvalidCredentialsError
            }
            .flatMap { credentials ->
                if (!credentials.isValid()) {
                    Either.Left(InvalidCredentialsError)
                } else {
                    Either.Right(credentials)
                }
            }
    }

    private fun refreshAndRetry(request: Request): Either<Failure, Request?> {
        Timber.d("[${request.path()}] Trying refresh & retry.")
        return getAuthToken()
            .tapLeft {
                Timber.d("[${request.path()}] Invalid refresh token. Cannot refresh.")
            }
            .flatMap { authToken ->
                Timber.d("[${request.path()}] Trying to refresh token.")
                runBlocking {
                    authService.refresh(RefreshBody(authToken.refresh)).map()
                        .tapLeft {
                            Timber.d("[${request.path()}] Token refresh failed.")
                        }
                        .map {
                            Timber.d("[${request.path()}] Token refresh success.")
                            retryWithAccessToken(request, it.access)
                        }
                }
            }
    }

    private fun loginAndRetry(request: Request): Either<Failure, Request?> {
        Timber.d("[${request.path()}] Trying login & retry.")
        return getCredentials()
            .tapLeft {
                Timber.d("[${request.path()}] Invalid credentials. Cannot login.")
            }
            .flatMap { credentials ->
                Timber.d("[${request.path()}] Trying to login.")
                runBlocking {
                    authService.login(LoginBody(credentials.username, credentials.password)).map()
                        .tapLeft {
                            Timber.d("[${request.path()}] Login failed.")
                        }
                        .map {
                            Timber.d("[${request.path()}] Login success.")
                            retryWithAccessToken(request, it.access)
                        }
                }
            }
    }

    private fun retryWithAccessToken(request: Request, accessToken: String): Request {
        Timber.d("[${request.path()}] Retrying with new access token.")
        return request.newBuilder()
            .header(AUTH_HEADER, "Bearer $accessToken")
            .build()
    }
}
