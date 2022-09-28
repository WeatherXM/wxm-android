package com.weatherxm.data.datasource.bluetooth

import android.net.Uri
import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.bluetooth.BluetoothUpdater
import kotlinx.coroutines.flow.Flow

class BluetoothUpdaterDataSourceImpl(
    private val updater: BluetoothUpdater
) : BluetoothUpdaterDataSource {

    override fun registerOnUpdateCompletionStatus(): Flow<Either<Failure, Unit>> {
        return updater.getCompletionStatus()
    }

    override fun setUpdater() {
        updater.setUpdater()
    }

    override fun update(updatePackage: Uri): Flow<Int> {
        return updater.update(updatePackage)
    }
}
