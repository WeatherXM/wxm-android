package com.weatherxm.data

import androidx.annotation.Keep

/**
 * Base Class for handling errors/failures/exceptions.
 */
@Keep
sealed class Failure {
    object NetworkError : Failure()
    object UnknownError : Failure()
}

@Keep
sealed class ServerError(val message: String? = null) : Failure() {
    class GenericError(message: String? = null) : ServerError(message)
    class InternalError(message: String? = null) : ServerError(message)
    object Unauthorized : ServerError(null)
    object Forbidden : ServerError()
    object NotFound : ServerError()
    object Unavailable : ServerError()
    object Timeout : ServerError()
    object JsonError : ServerError()
    object BadRequest : ServerError()
}

@Keep
sealed class DeviceFailure : Failure() {
    object NoData : DeviceFailure()
}
