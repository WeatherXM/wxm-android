package com.weatherxm.data.repository.bluetooth

import android.net.Uri
import com.weatherxm.data.OTAState
import kotlinx.coroutines.flow.Flow

interface BluetoothUpdaterRepository {
    fun update(updatePackage: Uri): Flow<OTAState>
}
