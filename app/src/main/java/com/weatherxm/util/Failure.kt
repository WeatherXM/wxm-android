package com.weatherxm.util

import androidx.annotation.StringRes
import com.weatherxm.R
import com.weatherxm.data.models.ApiError.GenericError.UnsupportedAppVersion
import com.weatherxm.data.models.ApiError.GenericError.ValidationError
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.Failure.TooManyRequestsError
import com.weatherxm.data.models.NetworkError.ConnectionTimeoutError
import com.weatherxm.data.models.NetworkError.NoConnectionError
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object Failure : KoinComponent {

    private val resources: Resources by inject()

    @StringRes
    fun Failure.getDefaultMessageResId(@StringRes fallback: Int? = null): Int {
        return when (this) {
            is UnsupportedAppVersion -> R.string.error_unsupported_error
            is NoConnectionError -> R.string.error_network_generic
            is ConnectionTimeoutError -> R.string.error_network_timed_out
            is ValidationError -> R.string.error_server_validation
            is TooManyRequestsError -> R.string.error_refreshed_too_quickly
            else -> fallback ?: R.string.error_reach_out
        }
    }

    fun Failure.getDefaultMessage(@StringRes fallback: Int? = null): String {
        return resources.getString(this.getDefaultMessageResId(fallback))
    }

    fun Failure.getCode(): String {
        return code ?: Failure.CODE_UNKNOWN
    }
}
