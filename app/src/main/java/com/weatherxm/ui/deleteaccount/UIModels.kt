package com.weatherxm.ui.deleteaccount

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass
import com.weatherxm.data.models.Failure

@Keep
@JsonClass(generateAdapter = true)
data class State(
    var status: Status,
    var failure: Failure? = null
)

enum class Status {
    PASSWORD_VERIFICATION,
    ACCOUNT_DELETION
}
