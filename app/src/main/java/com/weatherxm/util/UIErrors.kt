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
    fun Failure.getDefaultMessageResId(): Int {
        return when (this) {
            is NoConnectionError -> R.string.error_network_generic
            is ConnectionTimeoutError -> R.string.error_network_timed_out
            else -> R.string.error_generic_message
        }
    }

    fun Failure.getDefaultMessage(): String {
        return resHelper.getString(this.getDefaultMessageResId())
    }
}
