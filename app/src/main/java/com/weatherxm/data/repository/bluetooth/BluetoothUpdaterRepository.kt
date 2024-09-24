package com.weatherxm.data.repository.bluetooth

import android.net.Uri
import com.weatherxm.data.models.OTAState
import kotlinx.coroutines.flow.Flow

interface BluetoothUpdaterRepository {
    fun update(updatePackage: Uri): Flow<OTAState>
}
