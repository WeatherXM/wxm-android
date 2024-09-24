package com.weatherxm.usecases

import android.net.Uri
import arrow.core.Either
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.OTAState
import kotlinx.coroutines.flow.Flow

interface BluetoothUpdaterUseCase {
    suspend fun downloadFirmwareAndGetFileURI(deviceId: String): Either<Failure, Uri>
    fun update(updatePackage: Uri): Flow<OTAState>
    fun onUpdateSuccess(deviceId: String, otaVersion: String)
}
