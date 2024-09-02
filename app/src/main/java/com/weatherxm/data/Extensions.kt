package com.weatherxm.data

import arrow.core.Either
import com.auth0.android.jwt.JWT
import com.google.android.gms.tasks.Task
import com.haroldadmin.cnradapter.NetworkResponse
import com.squareup.moshi.JsonDataException
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
import com.weatherxm.data.NetworkError.ConnectionTimeoutError
import com.weatherxm.data.NetworkError.NoConnectionError
import com.weatherxm.data.NetworkError.ParseJsonError
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
import timber.log.Timber
import java.net.SocketTimeoutException

fun Request.path(): String = this.url.encodedPath

fun Response.path(): String = this.request.path()

/**
 * Map a NetworkResponse to Either using Failure sealed classes.
 * Suppress ComplexMethod because it is just a bunch of "when statements"
 */
@Suppress("ComplexMethod")
fun <T : Any> NetworkResponse<T, ErrorResponse>.map(): Either<Failure, T> {
    Timber.d("Mapping network response")
    return try {
        when (this) {
            is NetworkResponse.Success -> {
                Timber.d("Network response: Success")
                Either.Right(this.body)
            }
            is NetworkResponse.ServerError -> {
                Timber.d(this.error, "Network response: ServerError")
                Timber.w(this.error, this.body.toString())
                val code = this.body?.code
                Either.Left(
                    when (code) {
                        INVALID_USERNAME -> InvalidUsername(code, this.body?.message)
                        INVALID_PASSWORD -> InvalidPassword(code, this.body?.message)
                        INVALID_CREDENTIALS -> InvalidCredentials(code, this.body?.message)
                        USER_ALREADY_EXISTS -> UserAlreadyExists(code, this.body?.message)
                        INVALID_ACCESS_TOKEN -> InvalidAccessToken(code, this.body?.message)
                        INVALID_ACTIVATION_TOKEN -> InvalidActivationToken(code, this.body?.message)
                        DEVICE_NOT_FOUND -> DeviceNotFound(code, this.body?.message)
                        MAX_FOLLOWED -> MaxFollowed(code, this.body?.message)
                        INVALID_WALLET_ADDRESS -> InvalidWalletAddress(code, this.body?.message)
                        INVALID_FRIENDLY_NAME -> InvalidFriendlyName(code, this.body?.message)
                        INVALID_FROM_DATE -> InvalidFromDate(code, this.body?.message)
                        INVALID_TO_DATE -> InvalidToDate(code, this.body?.message)
                        INVALID_TIMEZONE -> InvalidTimezone(code, this.body?.message)
                        INVALID_CLAIM_ID -> InvalidClaimId(code, this.body?.message)
                        INVALID_CLAIM_LOCATION -> InvalidClaimLocation(code, this.body?.message)
                        DEVICE_ALREADY_CLAIMED -> DeviceAlreadyClaimed(code, this.body?.message)
                        DEVICE_CLAIMING -> DeviceClaiming(code, this.body?.message)
                        UNAUTHORIZED -> UnauthorizedError(code, this.body?.message)
                        USER_NOT_FOUND -> UserNotFoundError(code, this.body?.message)
                        FORBIDDEN -> ForbiddenError(code, this.body?.message)
                        VALIDATION -> ValidationError(code, this.body?.message)
                        NOT_FOUND -> NotFoundError(code, this.body?.message)
                        WALLET_ADDRESS_NOT_FOUND -> WalletAddressNotFound(code, this.body?.message)
                        UNSUPPORTED_APPLICATION_VERSION -> {
                            UnsupportedAppVersion(code, this.body?.message)
                        }
                        else -> UnknownError(code, this.body?.message)
                    }
                )
            }
            is NetworkResponse.NetworkError -> {
                if (this.error is SocketTimeoutException) {
                    Timber.d(this.error, "Network response: ConnectionTimeoutError")
                    Either.Left(ConnectionTimeoutError())
                } else {
                    Timber.d(this.error, "Network response: NoConnectionError")
                    Either.Left(NoConnectionError())
                }
            }
            is NetworkResponse.UnknownError -> {
                Timber.d(this.error, "Network response: UnknownError")
                Either.Left(UnknownError())
            }
        }
    } catch (exception: JsonDataException) {
        Timber.w(exception, "Could not parse json response")
        Either.Left(ParseJsonError())
    }
}

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
    return "token=${Mask.maskHash(this.toString())}, expires=${this.expiresAt}"
}
