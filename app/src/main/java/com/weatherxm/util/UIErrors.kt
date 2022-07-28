package com.weatherxm.util

import androidx.annotation.StringRes
import com.weatherxm.R
import com.weatherxm.data.Failure
import com.weatherxm.data.NetworkError.ConnectionTimeoutError
import com.weatherxm.data.NetworkError.NoConnectionError
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object UIErrors : KoinComponent {

    private val resHelper: ResourcesHelper by inject()

    @StringRes
    fun Failure.getDefaultMessageResId(@StringRes fallback: Int? = null): Int {
        return when (this) {
            is NoConnectionError -> R.string.error_network_generic
            is ConnectionTimeoutError -> R.string.error_network_timed_out
            else -> fallback ?: R.string.error_reach_out
        }
    }

    fun Failure.getDefaultMessage(@StringRes fallback: Int? = null): String {
        return resHelper.getString(this.getDefaultMessageResId(fallback))
    }
}
