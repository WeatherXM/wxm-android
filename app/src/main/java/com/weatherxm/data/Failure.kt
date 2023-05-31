package com.weatherxm.data

import androidx.annotation.Keep

/**
 * Base Class for handling errors/failures/exceptions.
 */
@Keep
sealed class Failure(val code: String? = null) {
    companion object {
        const val CODE_CONNECTION_TIMEOUT = "CONNECTION_TIMEOUT"
        const val CODE_NO_CONNECTION = "NO_CONNECTION"
        const val CODE_UNKNOWN = "UNKNOWN"
        const val CODE_JSON = "PARSE_JSON"
        const val CODE_BL_REJECTED = "BLUETOOTH_REJECTED"
        const val CODE_BL_CONNECTION_LOST = "BLUETOOTH_CONNECTION_LOST"
        const val CODE_BL_GATT_REQUEST_REJECTED = "GATT_REQUEST_REJECTED"
        const val CODE_BL_DISABLED = "BLUETOOTH_DISABLED"
        const val CODE_BL_CANCELLATION = "BLUETOOTH_CANCELLATION"
        const val CODE_PERIPHERAL_ERROR = "PERIPHERAL_ERROR"
        const val CODE_AT_COMMAND_ERROR = "AT_COMMAND_ERROR"
        const val CODE_USER_NOT_LOGGED_IN = "USER_NOT_LOGGED_IN"
    }

    object NoGeocoderError : Failure()
    object LocationAddressNotFound : Failure()
    object CountryNotFound : Failure()
    object FrequencyMappingNotFound : Failure()
    object UnknownError : Failure()
    object InvalidRefreshTokenError : Failure()
}

@Keep
sealed class NetworkError(code: String?) : Failure(code) {
    class ConnectionTimeoutError(code: String? = CODE_CONNECTION_TIMEOUT) : NetworkError(code)
    class NoConnectionError(code: String? = CODE_NO_CONNECTION) : NetworkError(code)
    class ParseJsonError(code: String? = CODE_JSON) : NetworkError(code)
}

@Keep
sealed class BluetoothError(code: String? = null, val message: String? = null) : Failure(code) {
    object ScanningError : BluetoothError()
    object DeviceNotFound : BluetoothError()
    object DfuAborted : BluetoothError()

    class ATCommandError(code: String? = CODE_AT_COMMAND_ERROR) : BluetoothError(code)
    class PeripheralCreationError(code: String? = CODE_PERIPHERAL_ERROR) : BluetoothError(code)
    class ConnectionRejectedError(code: String? = CODE_BL_REJECTED) : BluetoothError(code)
    class CancellationError(code: String? = CODE_BL_CANCELLATION) : BluetoothError(code)
    class ConnectionLostException(code: String? = CODE_BL_CONNECTION_LOST) : BluetoothError(code)
    class GattRequestRejectedException(
        code: String? = CODE_BL_GATT_REQUEST_REJECTED
    ) : BluetoothError(code)

    class BluetoothDisabledException(code: String? = CODE_BL_DISABLED) : BluetoothError(code)
    class DfuUpdateError(message: String? = null) : BluetoothError(message)

    sealed class ProvisionError : BluetoothError() {
        object GenericError : ProvisionError()
        object WifiConfigError : ProvisionError()
        object WifiScanError : ProvisionError()
        object CreateSessionError : ProvisionError()
    }
}

@Keep
sealed class ApiError(code: String?, val message: String? = null) : Failure(code) {
    sealed class AuthError(code: String?, message: String? = null) : ApiError(code, message) {
        sealed class LoginError(code: String?, message: String? = null) : AuthError(code, message) {
            class InvalidPassword(code: String?, message: String? = null) : AuthError(code, message)
            class InvalidCredentials(
                code: String?,
                message: String? = null
            ) : AuthError(code, message)
        }

        sealed class SignupError(code: String?, message: String? = null) :
            AuthError(code, message) {
            class UserAlreadyExists(
                code: String?,
                message: String? = null
            ) : AuthError(code, message)
        }

        class InvalidUsername(code: String?, message: String? = null) : AuthError(code, message)
        class InvalidAccessToken(code: String?, message: String? = null) : AuthError(code, message)
        class InvalidActivationToken(
            code: String?,
            message: String? = null
        ) : AuthError(code, message)
    }

    class DeviceNotFound(code: String?, message: String? = null) : ApiError(code, message)
    class InvalidFriendlyName(code: String?, message: String? = null) : ApiError(code, message)

    sealed class UserError(code: String?, message: String? = null) : ApiError(code, message) {
        sealed class WalletError(code: String?, message: String? = null) :
            UserError(code, message) {
            class InvalidWalletAddress(
                code: String?,
                message: String? = null
            ) : WalletError(code, message)
        }

        sealed class ClaimError(code: String?, message: String? = null) : UserError(code, message) {
            class InvalidClaimId(code: String?, message: String? = null) : ClaimError(code, message)
            class InvalidClaimLocation(
                code: String?,
                message: String? = null
            ) : ClaimError(code, message)

            class DeviceAlreadyClaimed(
                code: String?,
                message: String? = null
            ) : ClaimError(code, message)

            class DeviceClaiming(
                code: String?,
                message: String? = null
            ) : ClaimError(code, message)

            class UnknownClaimedDevice(
                code: String? = null,
                message: String? = null
            ) : ClaimError(code, message)

            class ClaimCancelledError(
                code: String? = null,
                message: String? = null
            ) : ClaimError(code, message)

            class ClaimUnknownError(
                code: String? = null,
                message: String? = null
            ) : ClaimError(code, message)
        }

        class InvalidFromDate(code: String?, message: String? = null) : UserError(code, message)
        class InvalidToDate(code: String?, message: String? = null) : UserError(code, message)
        class InvalidTimezone(code: String?, message: String? = null) : UserError(code, message)
    }

    sealed class GenericError(code: String?, message: String? = null) : ApiError(code, message) {
        sealed class JWTError(code: String?, message: String? = null) :
            GenericError(code, message) {
            class UnauthorizedError(code: String?, message: String? = null) :
                JWTError(code, message)

            class UserNotFoundError(code: String?, message: String? = null) :
                JWTError(code, message)

            class ForbiddenError(code: String?, message: String? = null) : JWTError(code, message)
        }

        class ValidationError(code: String?, message: String? = null) : GenericError(code, message)
        class UnknownError(
            code: String? = CODE_UNKNOWN,
            message: String? = null
        ) : GenericError(code, message)

        class NotFoundError(code: String?, message: String? = null) : GenericError(code, message)
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
    class UserNotLoggedInError(code: String = CODE_USER_NOT_LOGGED_IN) : Failure(code)
}

@Keep
sealed class MapBoxError : Failure() {
    object GeocodingError : MapBoxError()
    sealed class ReverseGeocodingError : MapBoxError() {
        object SearchResultNoAddressError : ReverseGeocodingError()
        object SearchResultNotAccurateError : ReverseGeocodingError()
        object SearchResultNotNearbyError : ReverseGeocodingError()
        object SearchResultAddressFormatError : ReverseGeocodingError()
    }

    object SuggestionLocationError : MapBoxError()
}

@Keep
object CancellationError : Failure()
