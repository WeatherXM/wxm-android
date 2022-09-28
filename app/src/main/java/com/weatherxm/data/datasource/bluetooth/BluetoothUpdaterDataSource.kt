package com.weatherxm.data.datasource.bluetooth

import android.net.Uri
import arrow.core.Either
import com.weatherxm.data.Failure
import kotlinx.coroutines.flow.Flow

interface BluetoothUpdaterDataSource {
    fun registerOnUpdateCompletionStatus(): Flow<Either<Failure, Unit>>
    fun setUpdater()
    fun update(updatePackage: Uri): Flow<Int>
}
