package com.weatherxm.ui.devicesettings

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass
import com.weatherxm.data.Failure

@Keep
@JsonClass(generateAdapter = true)
data class UIDeviceInfo(
    val title: String,
    val value: String,
    val action: UIDeviceAction? = null,
    val warning: String? = null
) {
    override fun toString(): String {
        return "$title: $value"
    }
}

@Keep
@JsonClass(generateAdapter = true)
data class UIDeviceAction(
    val actionText: String,
    val actionType: ActionType
)

@JsonClass(generateAdapter = true)
data class RebootState(
    var status: RebootStatus,
    var failure: Failure? = null
)

enum class RebootStatus {
    SCAN_FOR_STATION,
    PAIR_STATION,
    CONNECT_TO_STATION,
    REBOOTING
}

@JsonClass(generateAdapter = true)
data class ChangeFrequencyState(
    var status: FrequencyStatus,
    var failure: Failure? = null
)

enum class FrequencyStatus {
    SCAN_FOR_STATION,
    PAIR_STATION,
    CONNECT_TO_STATION,
    CHANGING_FREQUENCY
}

enum class ActionType {
    UPDATE_FIRMWARE
}
