package com.weatherxm.data.datasource.bluetooth

import android.net.Uri
import com.weatherxm.data.OTAState
import kotlinx.coroutines.flow.Flow

interface BluetoothUpdaterDataSource {
    fun update(updatePackage: Uri): Flow<OTAState>
}
