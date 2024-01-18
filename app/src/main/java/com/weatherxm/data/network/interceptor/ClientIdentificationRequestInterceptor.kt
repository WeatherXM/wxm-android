package com.weatherxm.data.network.interceptor

import com.weatherxm.data.ClientIdentificationHelper
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber

/**
 * {@see okhttp3.Interceptor} that adds User-Agent header to the request,
 * using app & device information.
 */
class ClientIdentificationRequestInterceptor(
    clientIdentificationHelper: ClientIdentificationHelper
) : Interceptor {

    companion object {
        private const val CLIENT_IDENTIFICATION_HEADER = "X-WXM-Client"
    }

    private val clientIdentifier = clientIdentificationHelper.getInterceptorClientIdentifier()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        Timber.d("Adding client identification header [$clientIdentifier]")
        val requestWithUserAgent = request.newBuilder()
            .header(CLIENT_IDENTIFICATION_HEADER, clientIdentifier)
            .build()
        return chain.proceed(requestWithUserAgent)
    }
}
