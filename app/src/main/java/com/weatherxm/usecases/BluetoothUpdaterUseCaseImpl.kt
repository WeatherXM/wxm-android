package com.weatherxm.usecases

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import arrow.core.Either
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.OTAState
import com.weatherxm.data.repository.DeviceOTARepository
import com.weatherxm.data.repository.bluetooth.BluetoothUpdaterRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okio.Buffer
import okio.sink
import java.io.File

class BluetoothUpdaterUseCaseImpl(
    private val context: Context,
    private val repo: BluetoothUpdaterRepository,
    private val deviceOTARepository: DeviceOTARepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : BluetoothUpdaterUseCase {

    override suspend fun downloadFirmwareAndGetFileURI(deviceId: String): Either<Failure, Uri> {
        return deviceOTARepository.getFirmware(deviceId).map {
            val updateFile = withContext(dispatcher) {
                File.createTempFile("ota_${deviceId}_file", ".zip", context.cacheDir)
            }
            updateFile.sink().write(Buffer().write(it), it.size.toLong())
            updateFile.toUri()
        }
    }

    override fun update(updatePackage: Uri): Flow<OTAState> {
        return repo.update(updatePackage)
    }

    override fun onUpdateSuccess(deviceId: String, otaVersion: String) {
        deviceOTARepository.onUpdateSuccess(deviceId, otaVersion)
    }
}
