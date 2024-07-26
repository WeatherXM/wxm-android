package com.weatherxm.data

import androidx.work.Constraints
import androidx.work.NetworkType
import arrow.core.Either
import com.auth0.android.jwt.JWT
import com.google.android.gms.tasks.Task
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import com.weatherxm.data.ApiError.AuthError.InvalidAccessToken
import com.weatherxm.data.ApiError.AuthError.InvalidActivationToken
import com.weatherxm.data.ApiError.AuthError.InvalidUsername
import com.weatherxm.data.ApiError.AuthError.LoginError.InvalidCredentials
import com.weatherxm.data.ApiError.AuthError.LoginError.InvalidPassword
import com.weatherxm.data.ApiError.AuthError.SignupError.UserAlreadyExists
import com.weatherxm.data.ApiError.DeviceNotFound
import com.weatherxm.data.ApiError.GenericError.JWTError.ForbiddenError
import com.weatherxm.data.ApiError.GenericError.JWTError.UnauthorizedError
import com.weatherxm.data.ApiError.GenericError.JWTError.UserNotFoundError
import com.weatherxm.data.ApiError.GenericError.NotFoundError
import com.weatherxm.data.ApiError.GenericError.UnknownError
import com.weatherxm.data.ApiError.GenericError.UnsupportedAppVersion
import com.weatherxm.data.ApiError.GenericError.ValidationError
import com.weatherxm.data.ApiError.InvalidFriendlyName
import com.weatherxm.data.ApiError.MaxFollowed
import com.weatherxm.data.ApiError.UserError.ClaimError.DeviceAlreadyClaimed
import com.weatherxm.data.ApiError.UserError.ClaimError.DeviceClaiming
import com.weatherxm.data.ApiError.UserError.ClaimError.InvalidClaimId
import com.weatherxm.data.ApiError.UserError.ClaimError.InvalidClaimLocation
import com.weatherxm.data.ApiError.UserError.InvalidFromDate
import com.weatherxm.data.ApiError.UserError.InvalidTimezone
import com.weatherxm.data.ApiError.UserError.InvalidToDate
import com.weatherxm.data.ApiError.UserError.WalletError.InvalidWalletAddress
import com.weatherxm.data.ApiError.UserError.WalletError.WalletAddressNotFound
import com.weatherxm.data.network.ErrorResponse
import com.weatherxm.data.network.ErrorResponse.Companion.DEVICE_ALREADY_CLAIMED
import com.weatherxm.data.network.ErrorResponse.Companion.DEVICE_CLAIMING
import com.weatherxm.data.network.ErrorResponse.Companion.DEVICE_NOT_FOUND
import com.weatherxm.data.network.ErrorResponse.Companion.FORBIDDEN
import com.weatherxm.data.network.ErrorResponse.Companion.INVALID_ACCESS_TOKEN
import com.weatherxm.data.network.ErrorResponse.Companion.INVALID_ACTIVATION_TOKEN
import com.weatherxm.data.network.ErrorResponse.Companion.INVALID_CLAIM_ID
import com.weatherxm.data.network.ErrorResponse.Companion.INVALID_CLAIM_LOCATION
import com.weatherxm.data.network.ErrorResponse.Companion.INVALID_CREDENTIALS
import com.weatherxm.data.network.ErrorResponse.Companion.INVALID_FRIENDLY_NAME
import com.weatherxm.data.network.ErrorResponse.Companion.INVALID_FROM_DATE
import com.weatherxm.data.network.ErrorResponse.Companion.INVALID_PASSWORD
import com.weatherxm.data.network.ErrorResponse.Companion.INVALID_TIMEZONE
import com.weatherxm.data.network.ErrorResponse.Companion.INVALID_TO_DATE
import com.weatherxm.data.network.ErrorResponse.Companion.INVALID_USERNAME
import com.weatherxm.data.network.ErrorResponse.Companion.INVALID_WALLET_ADDRESS
import com.weatherxm.data.network.ErrorResponse.Companion.MAX_FOLLOWED
import com.weatherxm.data.network.ErrorResponse.Companion.NOT_FOUND
import com.weatherxm.data.network.ErrorResponse.Companion.UNAUTHORIZED
import com.weatherxm.data.network.ErrorResponse.Companion.UNSUPPORTED_APPLICATION_VERSION
import com.weatherxm.data.network.ErrorResponse.Companion.USER_ALREADY_EXISTS
import com.weatherxm.data.network.ErrorResponse.Companion.USER_NOT_FOUND
import com.weatherxm.data.network.ErrorResponse.Companion.VALIDATION
import com.weatherxm.data.network.ErrorResponse.Companion.WALLET_ADDRESS_NOT_FOUND
import com.weatherxm.util.Mask
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import okhttp3.Request
import okhttp3.Response
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import java.net.SocketTimeoutException

val errorResponseAdapter: JsonAdapter<ErrorResponse> =
    Moshi.Builder().build().adapter(ErrorResponse::class.java)

/**
 * Map a network response containing Throwable to Either using Failure sealed classes as left.
 * Suppress ComplexMethod because it is just a bunch of "when statements"
 */
@Suppress("ComplexMethod")
fun <T : Any> Either<Throwable, T>.leftToFailure(): Either<Failure, T> {
    return mapLeft {
        Timber.d(it, "Network response: Error")
        when (it) {
            is IOException -> {
                if (it is SocketTimeoutException) {
                    Timber.d(it, "Network response: ConnectionTimeoutError")
                    NetworkError.ConnectionTimeoutError()
                } else {
                    Timber.d(it, "Network response: NoConnectionError")
                    NetworkError.NoConnectionError()
                }
            }
            is HttpException -> {
                Timber.d(it, "Network response: ServerError")
                Timber.w(it, it.toString())

                val error = try {
                    errorResponseAdapter.fromJson(it.response()?.errorBody()?.string() ?: "")
                } catch (e: JsonDataException) {
                    Timber.e(e, "Network response: JsonDataException")
                    ErrorResponse.empty()
                }
                val code = error?.code
                val message = error?.message

                when (code) {
                    INVALID_USERNAME -> InvalidUsername(code, message)
                    INVALID_PASSWORD -> InvalidPassword(code, message)
                    INVALID_CREDENTIALS -> InvalidCredentials(code, message)
                    USER_ALREADY_EXISTS -> UserAlreadyExists(code, message)
                    INVALID_ACCESS_TOKEN -> InvalidAccessToken(code, message)
                    INVALID_ACTIVATION_TOKEN -> InvalidActivationToken(code, message)
                    DEVICE_NOT_FOUND -> DeviceNotFound(code, message)
                    MAX_FOLLOWED -> MaxFollowed(code, message)
                    INVALID_WALLET_ADDRESS -> InvalidWalletAddress(code, message)
                    INVALID_FRIENDLY_NAME -> InvalidFriendlyName(code, message)
                    INVALID_FROM_DATE -> InvalidFromDate(code, message)
                    INVALID_TO_DATE -> InvalidToDate(code, message)
                    INVALID_TIMEZONE -> InvalidTimezone(code, message)
                    INVALID_CLAIM_ID -> InvalidClaimId(code, message)
                    INVALID_CLAIM_LOCATION -> InvalidClaimLocation(code, message)
                    DEVICE_ALREADY_CLAIMED -> DeviceAlreadyClaimed(code, message)
                    DEVICE_CLAIMING -> DeviceClaiming(code, message)
                    UNAUTHORIZED -> UnauthorizedError(code, message)
                    USER_NOT_FOUND -> UserNotFoundError(code, message)
                    FORBIDDEN -> ForbiddenError(code, message)
                    VALIDATION -> ValidationError(code, message)
                    NOT_FOUND -> NotFoundError(code, message)
                    WALLET_ADDRESS_NOT_FOUND -> WalletAddressNotFound(code, message)
                    UNSUPPORTED_APPLICATION_VERSION -> {
                        UnsupportedAppVersion(code, message)
                    }
                    else -> UnknownError(code, message)
                }
            }
            else -> {
                Timber.e(it, "Network response: UnknownError")
                UnknownError()
            }
        }
    }
}

fun Request.path(): String = this.url.encodedPath

fun Response.path(): String = this.request.path()

/**
 * Await the completion of the task, blocking the thread.
 * Returns the result, wrapped in [Either.Right], if the task is successful,
 * or a throwable, wrapped in [Either.Left], if the task fails.
 */
fun <T> Task<T>.safeAwait(): Either<Throwable, T> = Either.catch {
    runBlocking {
        this@safeAwait.await()
    }
}

fun JWT.details(): String {
    return "JWT token=${Mask.maskHash(this.toString())}, expires=${this.expiresAt}"
}

fun Constraints.Companion.requireNetwork(): Constraints = Constraints.Builder()
    .setRequiredNetworkType(NetworkType.CONNECTED)
    .build()
