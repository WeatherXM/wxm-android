package com.weatherxm.data

import arrow.core.Either
import com.haroldadmin.cnradapter.NetworkResponse
import com.squareup.moshi.JsonDataException
import com.weatherxm.data.network.ErrorResponse
import okhttp3.Request
import okhttp3.Response
import timber.log.Timber
import java.net.HttpURLConnection.HTTP_FORBIDDEN
import java.net.HttpURLConnection.HTTP_GATEWAY_TIMEOUT
import java.net.HttpURLConnection.HTTP_INTERNAL_ERROR
import java.net.HttpURLConnection.HTTP_NOT_FOUND
import java.net.HttpURLConnection.HTTP_UNAUTHORIZED
import java.net.HttpURLConnection.HTTP_UNAVAILABLE
import java.net.HttpURLConnection.HTTP_BAD_REQUEST

fun Request.path(): String = this.url.encodedPath

fun Response.path(): String = this.request.path()

/**
 * Map a NetworkResponse to Either using Failure sealed classes.
 */
fun <T : Any> NetworkResponse<T, ErrorResponse>.map(): Either<Failure, T> {
    Timber.d("Mapping network response")
    return try {
        when (this) {
            is NetworkResponse.Success -> {
                Timber.d("Network response: Success")
                Either.Right(this.body)
            }
            is NetworkResponse.ServerError -> {
                Timber.d("Network response: ServerError: ", this.error)
                when (this.code) {
                    HTTP_UNAUTHORIZED -> Either.Left(ServerError.Unauthorized)
                    HTTP_FORBIDDEN -> Either.Left(ServerError.Forbidden)
                    HTTP_NOT_FOUND -> Either.Left(ServerError.NotFound)
                    HTTP_INTERNAL_ERROR -> Either.Left(ServerError.InternalError(this.body?.message))
                    HTTP_UNAVAILABLE -> Either.Left(ServerError.Unavailable)
                    HTTP_GATEWAY_TIMEOUT -> Either.Left(ServerError.Timeout)
                    HTTP_BAD_REQUEST -> Either.Left(ServerError.BadRequest)
                    else -> Either.Left(ServerError.GenericError(this.body?.message))
                }
            }
            is NetworkResponse.NetworkError -> {
                Timber.d("Network response: NetworkError: ", this.error)
                Either.Left(Failure.NetworkError)
            }
            is NetworkResponse.UnknownError -> {
                Timber.d("Network response: UnknownError: ", this.error)
                Either.Left(Failure.UnknownError)
            }
        }
    } catch (exception: JsonDataException) {
        Timber.d(exception, "Could not parse json response")
        Either.Left(ServerError.JsonError)
    }
}
