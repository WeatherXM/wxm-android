package com.weatherxm.ui.common

import com.weatherxm.data.models.Failure

/**
 * Status of a resource that is provided to the UI.
 *
 *
 * These are usually created by the Repository classes where they return
 * `LiveData<Resource<T>>` to pass back the latest data to the UI with its fetch status.
 */
enum class Status {
    SUCCESS,
    ERROR,
    LOADING
}

/**
 * A generic class that holds a value with its loading status.
 */
data class Resource<out T>(
    val status: Status,
    val data: T?,
    val message: String?,
    val error: Failure?
) {
    companion object {
        fun <T> success(data: T?): Resource<T> {
            return Resource(Status.SUCCESS, data, null, null)
        }

        fun <T> error(msg: String, data: T? = null): Resource<T> {
            return Resource(Status.ERROR, data, msg, null)
        }

        fun <T> error(msg: String, error: Failure, data: T? = null): Resource<T> {
            return Resource(Status.ERROR, data, msg, error)
        }

        fun <T> loading(data: T? = null): Resource<T> {
            return Resource(Status.LOADING, data, null, null)
        }

        fun <T> empty(): Resource<T> {
            return success(null)
        }
    }
}
