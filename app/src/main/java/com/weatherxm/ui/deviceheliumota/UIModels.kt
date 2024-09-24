package com.weatherxm.ui.deviceheliumota

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass
import com.weatherxm.data.models.Failure

@Keep
@JsonClass(generateAdapter = true)
data class State(
    var status: OTAStatus,
    var failure: Failure? = null,
    var otaError: Int? = null,
    var otaErrorType: Int? = null,
    var otaErrorMessage: String? = null
)

enum class OTAStatus {
    SCAN_FOR_STATION,
    PAIR_STATION,
    CONNECT_TO_STATION,
    DOWNLOADING,
    INSTALLING
}
