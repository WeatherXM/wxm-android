package com.weatherxm.data

import androidx.annotation.Keep

/**
 * Base Class for handling errors/failures/exceptions.
 */
@Keep
sealed class Failure {
    object JsonError : Failure()
    object NoGeocoderError : Failure()
    object LocationAddressNotFound : Failure()
    object UnknownError : Failure()
}

@Keep
sealed class NetworkError : Failure() {
    object ConnectionTimeoutError : NetworkError()
    object NoConnectionError : NetworkError()
}

@Keep
sealed class BluetoothError(val message: String? = null) : Failure() {
    object PeripheralCreationError : BluetoothError()
    object ConnectionRejectedError : BluetoothError()
    object CancellationError : BluetoothError()
    object ConnectionLostException : BluetoothError()
    object BluetoothDisabledException : BluetoothError()
    object DfuAborted : BluetoothError()

    class DfuUpdateError(message: String? = null) : BluetoothError(message)
}

@Keep
sealed class AuthError : Failure() {
    object InvalidAuthTokenError : AuthError()
    object InvalidAccessTokenError : AuthError()
    object InvalidRefreshTokenError : AuthError()
    object InvalidCredentialsError : AuthError()
}

@Keep
sealed class ApiError(val message: String? = null) : Failure() {
    sealed class AuthError(message: String? = null) : ApiError(message) {
        sealed class LoginError(message: String? = null) : AuthError(message) {
            class InvalidPassword(message: String? = null) : AuthError(message)
            class InvalidCredentials(message: String? = null) : AuthError(message)
        }

        sealed class SignupError(message: String? = null) : AuthError(message) {
            class UserAlreadyExists(message: String? = null) : AuthError(message)
        }

        class InvalidUsername(message: String? = null) : AuthError(message)
        class InvalidAccessToken(message: String? = null) : AuthError(message)
    }

    class DeviceNotFound(message: String? = null) : ApiError(message)
    class InvalidFriendlyName(message: String? = null) : ApiError(message)

    sealed class UserError(message: String? = null) : ApiError(message) {
        sealed class WalletError(message: String? = null) : UserError(message) {
            class InvalidWalletAddress(message: String? = null) : WalletError(message)
        }

        sealed class ClaimError(message: String? = null) : UserError(message) {
            class InvalidClaimId(message: String? = null) : ClaimError(message)
            class InvalidClaimLocation(message: String? = null) : ClaimError(message)
            class DeviceAlreadyClaimed(message: String? = null) : ClaimError(message)
        }

        class InvalidFromDate(message: String? = null) : UserError(message)
        class InvalidToDate(message: String? = null) : UserError(message)
        class InvalidTimezone(message: String? = null) : UserError(message)
    }

    sealed class GenericError(message: String? = null) : ApiError(message) {
        sealed class JWTError(message: String? = null) : GenericError(message) {
            class UnauthorizedError(message: String? = null) : JWTError(message)
            class ForbiddenError(message: String? = null) : JWTError(message)
        }

        class ValidationError(message: String? = null) : GenericError(message)
        class UnknownError(message: String? = null) : GenericError(message)
        class NotFoundError(message: String? = null) : GenericError(message)
    }
}

@Keep
sealed class DataError : Failure() {
    object DatabaseMissError : DataError()
    object CacheMissError : DataError()
    object CacheExpiredError : DataError()
    object NoWalletAddressError : DataError()
}

@Keep
sealed class UserActionError(val message: String? = null) : Failure() {
    class UserActionRateLimitedError(message: String? = null) : UserActionError(message)
}
