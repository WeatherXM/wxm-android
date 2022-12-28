package com.weatherxm.data.repository.bluetooth

import android.net.Uri
import com.weatherxm.data.OTAState
import com.weatherxm.data.datasource.bluetooth.BluetoothUpdaterDataSource
import kotlinx.coroutines.flow.Flow

class BluetoothUpdaterRepositoryImpl(
    private val updaterSource: BluetoothUpdaterDataSource
) : BluetoothUpdaterRepository {

    override fun update(updatePackage: Uri): Flow<OTAState> {
        return updaterSource.update(updatePackage)
    }
}
