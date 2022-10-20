@file:Suppress("MatchingDeclarationName")
package com.weatherxm.data

import android.bluetooth.BluetoothDevice
import android.os.Parcelable
import androidx.annotation.Keep
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@Suppress("MagicNumber")
@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class LastAndDatedTxs(
    val lastTx: Transaction?,
    val datedTxs: List<Pair<String, Float>>,
) : Parcelable

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class BluetoothDeviceWithEUI(
    val devEUI: String?,
    val bluetoothDevice: BluetoothDevice,
) : Parcelable
