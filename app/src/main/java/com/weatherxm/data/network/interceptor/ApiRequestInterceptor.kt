package com.weatherxm.data.network.interceptor

import arrow.core.getOrElse
import com.weatherxm.data.datasource.CacheAuthDataSource
import com.weatherxm.data.network.interceptor.ApiRequestInterceptor.Companion.AUTH_HEADER
import com.weatherxm.data.path
import com.weatherxm.ui.common.empty
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import timber.log.Timber

/**
 * {@see okhttp3.Interceptor} that adds Authorization header to the request,
 * using a stored JWT token with Bearer schema.
 */
class ApiRequestInterceptor(private val cacheAuthDataSource: CacheAuthDataSource) : Interceptor {

    companion object {
        const val AUTH_HEADER = "Authorization"
        const val NO_AUTH_HEADER_KEY = "No-Authorization"
        const val NO_AUTH_HEADER_VALUE = "true"
        const val NO_AUTH_HEADER = "$NO_AUTH_HEADER_KEY: $NO_AUTH_HEADER_VALUE"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        // Original request
        val request: Request = chain.request()
        if (request.headers[NO_AUTH_HEADER_KEY] != null
            && request.headers[NO_AUTH_HEADER_KEY].equals(NO_AUTH_HEADER_VALUE)
        ) {
            return chain.proceed(request)
        }

        // Add auth header using the auth token with Bearer schema
        Timber.d("[${request.path()}] Adding Authorization header.")
        return chain.proceed(request.signedRequest(cacheAuthDataSource))
    }
}

@Suppress("TooGenericExceptionThrown")
private fun Request.signedRequest(cacheAuthDataSource: CacheAuthDataSource): Request {
    return runCatching {
        // Get stored auth token
        val authToken = runBlocking {
            cacheAuthDataSource.getAuthToken()
        }.getOrElse {
            throw Error("Auth Token not found.")
        }

        // If token is invalid, log, but proceed anyway
        if (!authToken.isAccessTokenValid()) {
            Timber.w("[${this.path()}] Invalid token ${authToken.access}.")
        }

        // Return a new request, adding auth header with access token
        return this.newBuilder()
            .header(AUTH_HEADER, "Bearer ${authToken.access}")
            .build()

    }.getOrElse {
        Timber.w(it, "[${this.path()}] Could not get token. Adding empty Auth header.")

        // Return a new request, adding empty auth header
        this.newBuilder()
            .header(AUTH_HEADER, String.empty())
            .build()
    }
}
