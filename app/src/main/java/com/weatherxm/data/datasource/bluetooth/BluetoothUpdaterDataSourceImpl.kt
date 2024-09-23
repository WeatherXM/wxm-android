package com.weatherxm.data.datasource.bluetooth

import android.net.Uri
import com.weatherxm.data.models.OTAState
import com.weatherxm.data.bluetooth.BluetoothUpdater
import kotlinx.coroutines.flow.Flow

class BluetoothUpdaterDataSourceImpl(
    private val updater: BluetoothUpdater
) : BluetoothUpdaterDataSource {

    override fun update(updatePackage: Uri): Flow<OTAState> {
        return updater.update(updatePackage)
    }
}
