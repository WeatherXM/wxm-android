package com.weatherxm.usecases

import android.net.Uri
import com.weatherxm.data.OTAState
import kotlinx.coroutines.flow.Flow

interface BluetoothUpdaterUseCase {
    fun update(updatePackage: Uri): Flow<OTAState>
}
