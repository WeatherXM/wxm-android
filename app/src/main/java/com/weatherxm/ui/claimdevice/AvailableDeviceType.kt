package com.weatherxm.ui.claimdevice

import android.os.Parcelable
import androidx.annotation.Keep
import com.squareup.moshi.JsonClass
import com.weatherxm.ui.common.DeviceType
import kotlinx.parcelize.Parcelize

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class AvailableDeviceType(
    val title: String,
    val desc: String,
    val type: DeviceType = DeviceType.HELIUM,
) : Parcelable
