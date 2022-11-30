package com.weatherxm.usecases

import android.net.Uri
import com.weatherxm.data.OTAState
import com.weatherxm.data.repository.bluetooth.BluetoothUpdaterRepository
import kotlinx.coroutines.flow.Flow

class BluetoothUpdaterUseCaseImpl(
    private val repo: BluetoothUpdaterRepository,
) : BluetoothUpdaterUseCase {

    override fun update(updatePackage: Uri): Flow<OTAState> {
        return repo.update(updatePackage)
    }
}
