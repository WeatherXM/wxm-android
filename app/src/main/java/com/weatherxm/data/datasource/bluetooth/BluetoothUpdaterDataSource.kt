package com.weatherxm.data.datasource.bluetooth

import android.net.Uri
import arrow.core.Either
import com.weatherxm.data.Failure
import kotlinx.coroutines.flow.Flow

interface BluetoothUpdaterDataSource {
    suspend fun setUpdater(): Either<Failure, Unit>
    fun update(updatePackage: Uri): Flow<Int>
}
