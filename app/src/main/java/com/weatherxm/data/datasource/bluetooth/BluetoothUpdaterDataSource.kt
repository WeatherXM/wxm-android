package com.weatherxm.data.datasource.bluetooth

import android.net.Uri
import kotlinx.coroutines.flow.Flow

interface BluetoothUpdaterDataSource {
    suspend fun setUpdater()
    fun update(updatePackage: Uri): Flow<Int>
}
