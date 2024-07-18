package com.weatherxm.data.bluetooth

import com.juul.kable.Advertisement
import com.juul.kable.Filter
import com.juul.kable.Scanner
import com.juul.kable.logs.Logging
import com.juul.kable.logs.SystemLogEngine
import kotlinx.coroutines.flow.Flow

class BluetoothScanner {
    private val scanner = Scanner {
        filters = listOf(Filter.NamePrefix("WeatherXM"), Filter.NamePrefix("DfuTarg"))
        logging {
            engine = SystemLogEngine
            level = Logging.Level.Warnings
            format = Logging.Format.Multiline
        }
    }

    fun scan(): Flow<Advertisement> = scanner.advertisements
}
