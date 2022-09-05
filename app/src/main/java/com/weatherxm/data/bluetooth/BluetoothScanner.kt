package com.weatherxm.data.bluetooth

import com.benasher44.uuid.uuidFrom
import com.juul.kable.Advertisement
import com.juul.kable.Filter
import com.juul.kable.Scanner
import kotlinx.coroutines.flow.Flow

class BluetoothScanner {
    private val scanner = Scanner {
        filters = listOf(
            Filter.Service(uuidFrom("00002886-0000-1000-8000-00805F9B34FB")),
            Filter.Service(uuidFrom("0000A886-0000-1000-8000-00805F9B34FB"))
        )
    }

    fun scanBleDevices(): Flow<Advertisement> {
        return scanner.advertisements
    }
}
