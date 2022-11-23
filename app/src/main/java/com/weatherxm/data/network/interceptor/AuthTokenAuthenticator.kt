package com.weatherxm.data.network.interceptor

import android.content.Context
import arrow.core.Either
import arrow.core.flatMap
import com.weatherxm.data.Failure
import com.weatherxm.data.datasource.CacheAuthDataSource
import com.weatherxm.data.map
import com.weatherxm.data.network.AuthService
import com.weatherxm.data.network.AuthToken
import com.weatherxm.data.network.RefreshBody
import com.weatherxm.data.network.interceptor.ApiRequestInterceptor.Companion.AUTH_HEADER
import com.weatherxm.data.path
import com.weatherxm.data.services.CacheService
import com.weatherxm.ui.Navigator
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import timber.log.Timber

class AuthTokenAuthenticator(
    private val authService: AuthService,
    private val cacheAuthDataSource: CacheAuthDataSource,
    private val cacheService: CacheService,
    private val navigator: Navigator,
    private val context: Context,
) : Authenticator {
    private lateinit var refreshJob: Deferred<AuthToken?>

    override fun authenticate(route: Route?, response: Response): Request? {
        // The original request
        val request = response.request

        Timber.d("[${request.path()}] Status: ${response.code}. Invoking authenticator.")

        // TODO: Is runBlocking correct here?
        return runBlocking {
            if (!this@AuthTokenAuthenticator::refreshJob.isInitialized || !refreshJob.isActive) {
                refreshJob = async {
                    refresh(request)
                        .tapLeft {
                            Timber.w("[${request.path()}] Failed to authenticate. Forced Logout.")
                            runBlocking {
                                authService.logout()
                                cacheService.clearAll()
                                navigator.showLogin(context, true)
                            }
                        }
                        .orNull()
                }
            }

            val newAuthToken = refreshJob.await()
            newAuthToken?.let {
                retryWithAccessToken(request, it.access)
            }
        }
    }

    private fun getAuthToken(): Either<Failure, AuthToken> = runBlocking {
        cacheAuthDataSource.getAuthToken()
            .flatMap { authToken ->
                if (!authToken.isRefreshTokenValid()) {
                    Either.Left(Failure.InvalidRefreshTokenError)
                } else {
                    Either.Right(authToken)
                }
            }
    }

    private fun refresh(request: Request): Either<Failure, AuthToken> {
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
                        .tap {
                            Timber.d("[${request.path()}] Token refresh success.")
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
