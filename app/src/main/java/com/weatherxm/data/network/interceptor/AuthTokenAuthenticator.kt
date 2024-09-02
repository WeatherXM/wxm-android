package com.weatherxm.data.network.interceptor

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import arrow.core.Either
import arrow.core.flatMap
import com.weatherxm.R
import com.weatherxm.data.Failure
import com.weatherxm.data.datasource.CacheAuthDataSource
import com.weatherxm.data.datasource.DatabaseExplorerDataSource
import com.weatherxm.data.leftToFailure
import com.weatherxm.data.network.AccessTokenBody
import com.weatherxm.data.map
import com.weatherxm.data.network.AuthService
import com.weatherxm.data.network.AuthToken
import com.weatherxm.data.network.RefreshBody
import com.weatherxm.data.network.interceptor.ApiRequestInterceptor.Companion.AUTH_HEADER
import com.weatherxm.data.path
import com.weatherxm.data.services.CacheService
import com.weatherxm.ui.Navigator
import com.weatherxm.ui.common.Contracts
import com.weatherxm.util.WidgetHelper
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import timber.log.Timber

@Suppress("LongParameterList")
class AuthTokenAuthenticator(
    private val authService: AuthService,
    private val cacheAuthDataSource: CacheAuthDataSource,
    private val databaseExplorerDataSource: DatabaseExplorerDataSource,
    private val cacheService: CacheService,
    private val navigator: Navigator,
    private val context: Context,
    private val widgetHelper: WidgetHelper
) : Authenticator {
    private lateinit var refreshJob: Deferred<AuthToken?>

    override fun authenticate(route: Route?, response: Response): Request? {
        // The original request
        val request = response.request

        Timber.d("[${request.path()}] Status: ${response.code}. Invoking authenticator.")

        return runBlocking {
            if (!this@AuthTokenAuthenticator::refreshJob.isInitialized || !refreshJob.isActive) {
                refreshJob = async {
                    refresh(request)
                        .onLeft {
                            Timber.w("[${request.path()}] Failed to authenticate. Forced Logout.")
                            runBlocking {
                                cacheService.getAuthToken().onRight {
                                    authService.logout(AccessTokenBody(it.access))
                                }
                                databaseExplorerDataSource.deleteAll()
                                cacheService.clearAll()
                                widgetHelper.getWidgetIds().onRight {
                                    val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                                    val ids = it.map { id ->
                                        id.toInt()
                                    }
                                    intent.putExtra(
                                        AppWidgetManager.EXTRA_APPWIDGET_IDS,
                                        ids.toIntArray()
                                    )
                                    intent.putExtra(Contracts.ARG_IS_CUSTOM_APPWIDGET_UPDATE, true)
                                    intent.putExtra(Contracts.ARG_WIDGET_SHOULD_LOGIN, true)
                                    context.sendBroadcast(intent)
                                }
                                navigator.showLogin(
                                    context,
                                    true,
                                    context.getString(R.string.session_expired)
                                )
                            }
                        }
                        .getOrNull()
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
                    authService.refresh(RefreshBody(authToken.refresh)).map()
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
