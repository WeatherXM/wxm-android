package com.weatherxm.data.network.interceptor

import com.weatherxm.data.datasource.AuthTokenDataSource
import com.weatherxm.data.network.AuthTokenJsonAdapter
import com.weatherxm.data.path
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import timber.log.Timber
import java.io.IOException

/**
 * {@see okhttp3.Interceptor} that saves an AuthToken in the response of the /auth endpoints.
 */
class AuthRequestInterceptor(
    private val authTokenDataSource: AuthTokenDataSource,
    private val authTokenJsonAdapter: AuthTokenJsonAdapter
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        // Original request
        val request: Request = chain.request()

        // Try to extract auth token from response before proceeding
        return extractAuthToken(
            chain.proceed(request)
        )
    }

    private fun extractAuthToken(response: Response): Response {
        if (response.isSuccessful) {
            try {
                Timber.d("[${response.path()}] Trying to extract auth token.")
                val body = response.peekBody(Long.MAX_VALUE).string()
                val authToken = authTokenJsonAdapter.fromJson(body)
                authToken?.let {
                    Timber.d("[${response.path()}] Saving token from response.")
                    runBlocking { authTokenDataSource.setAuthToken(authToken) }
                }
            } catch (e: IOException) {
                Timber.d(e, "[${response.path()}] Failed to extract auth token.")
            }
        }
        return response
    }
}
