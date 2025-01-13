package com.weatherxm.data.bluetooth

import com.juul.kable.Advertisement
import com.juul.kable.Filter
import com.juul.kable.InternalError
import com.juul.kable.Scanner
import com.juul.kable.UnmetRequirementException
import com.juul.kable.logs.Logging
import com.juul.kable.logs.SystemLogEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import timber.log.Timber

class BluetoothScanner {
    private val scanner = Scanner {
        filters {
            match {
                name = Filter.Name.Prefix("WeatherXM")
            }
        }
        logging {
            engine = SystemLogEngine
            level = Logging.Level.Warnings
            format = Logging.Format.Multiline
        }
    }

    fun scan(): Flow<Advertisement> {
        return try {
            scanner.advertisements
        } catch (e: IllegalStateException) {
            Timber.e(e, "[Scanning Error] IllegalStateException: ${e.message}")
            flowOf()
        } catch (e: UnmetRequirementException) {
            Timber.e(e, "[Scanning Error] UnmetRequirementException: ${e.message}")
            flowOf()
        } catch (e: InternalError) {
            Timber.e(e, "[Scanning Error] InternalException: ${e.message}")
            flowOf()
        }
    }
}
