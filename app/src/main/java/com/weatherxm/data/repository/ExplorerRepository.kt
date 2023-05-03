package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.Location
import com.weatherxm.data.PublicDevice
import com.weatherxm.data.PublicHex
import com.weatherxm.data.datasource.ExplorerDataSource
import com.weatherxm.data.datasource.NetworkAddressDataSource
import com.weatherxm.data.datasource.StorageAddressDataSource
import timber.log.Timber

interface ExplorerRepository {
    suspend fun getPublicHexes(): Either<Failure, List<PublicHex>>
    suspend fun getPublicDevicesOfHex(index: String): Either<Failure, List<PublicDevice>>
    suspend fun getPublicDevice(index: String, deviceId: String): Either<Failure, PublicDevice>
    suspend fun getAddressFromLocation(hexIndex: String, location: Location): String?
}

class ExplorerRepositoryImpl(
    private val explorerDataSource: ExplorerDataSource,
    private val networkAddressDataSource: NetworkAddressDataSource,
    private val storageAddressDataSource: StorageAddressDataSource
) : ExplorerRepository {

    override suspend fun getPublicHexes(): Either<Failure, List<PublicHex>> {
        return explorerDataSource.getPublicHexes()
    }

    override suspend fun getPublicDevicesOfHex(index: String): Either<Failure, List<PublicDevice>> {
        return explorerDataSource.getPublicDevicesOfHex(index)
    }

    override suspend fun getPublicDevice(
        index: String,
        deviceId: String
    ): Either<Failure, PublicDevice> {
        return explorerDataSource.getPublicDevice(index, deviceId)
    }

    override suspend fun getAddressFromLocation(hexIndex: String, location: Location): String? {
        var hexAddress: String? = null

        storageAddressDataSource.getLocationAddress(hexIndex, location)
            .onRight { address ->
                Timber.d("Got location address from database [$address].")
                hexAddress = address
            }
            .mapLeft {
                networkAddressDataSource.getLocationAddress(hexIndex, location).onRight { address ->
                    Timber.d("Got location address from network [$it].")
                    hexAddress = address
                    address?.let {
                        storageAddressDataSource.setLocationAddress(hexIndex, it)
                    }
                }
            }

        return hexAddress
    }
}
