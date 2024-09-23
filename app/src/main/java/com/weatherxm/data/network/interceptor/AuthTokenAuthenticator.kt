package com.weatherxm.data.network.interceptor

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import arrow.core.Either
import arrow.core.flatMap
import arrow.core.getOrElse
import com.weatherxm.R
import com.weatherxm.data.models.Failure
import com.weatherxm.data.datasource.CacheAuthDataSource
import com.weatherxm.data.mapResponse
import com.weatherxm.data.network.AuthService
import com.weatherxm.data.network.AuthToken
import com.weatherxm.data.network.RefreshBody
import com.weatherxm.data.network.interceptor.ApiRequestInterceptor.Companion.AUTH_HEADER
import com.weatherxm.data.path
import com.weatherxm.service.workers.ForceLogoutWorker
import com.weatherxm.ui.Navigator
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import org.koin.core.component.KoinComponent
import timber.log.Timber

class AuthTokenAuthenticator(
    private val authService: AuthService,
    private val cacheAuthDataSource: CacheAuthDataSource,
    private val navigator: Navigator,
    private val context: Context,
) : Authenticator, KoinComponent {
    private lateinit var refreshJob: Deferred<AuthToken?>

    override fun authenticate(route: Route?, response: Response): Request? {
        // The original request
        val request = response.request
        Timber.d("[${request.path()}] Status: ${response.code}. Invoking authenticator.")

        return runBlocking {
            if (!this@AuthTokenAuthenticator::refreshJob.isInitialized || !refreshJob.isActive) {
                refreshJob = async {
                    refresh(request).getOrElse {
                        Timber.w("[${request.path()}] Failed to authenticate. Forced Logout.")
                        /**
                         * Init and invoke the work manager
                         * to delete FCM token from the server
                         */
                        val updateTokenWork = OneTimeWorkRequestBuilder<ForceLogoutWorker>().build()
                        WorkManager.getInstance(context).enqueue(updateTokenWork)
                        navigator.showLogin(
                            context, true, context.getString(R.string.session_expired)
                        )
                        return@getOrElse null
                    }
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
            .onLeft {
                Timber.d("[${request.path()}] Invalid refresh token. Cannot refresh.")
            }
            .flatMap { authToken ->
                Timber.d("[${request.path()}] Trying to refresh token.")
                runBlocking {
                    authService.refresh(RefreshBody(authToken.refresh)).mapResponse()
                        .onLeft {
                            Timber.d("[${request.path()}] Token refresh failed.")
                        }
                        .onRight {
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
