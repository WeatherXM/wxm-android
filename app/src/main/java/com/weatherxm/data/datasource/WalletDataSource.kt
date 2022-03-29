package com.weatherxm.data.datasource

import arrow.core.Either
import com.weatherxm.data.DataError
import com.weatherxm.data.Failure
import com.weatherxm.data.map
import com.weatherxm.data.network.AddressBody
import com.weatherxm.data.network.ApiService

interface WalletDataSource {
    suspend fun getWalletAddress(): Either<Failure, String?>
    suspend fun setWalletAddress(address: String): Either<Failure, Unit>
    suspend fun clear()
}

class NetworkWalletDataSource(private val apiService: ApiService) : WalletDataSource {

    override suspend fun getWalletAddress(): Either<Failure, String?> {
        return apiService.getWallet().map().map { it.address }
    }

    override suspend fun setWalletAddress(address: String): Either<Failure, Unit> {
        return apiService.setWallet(AddressBody(address)).map()
    }

    override suspend fun clear() {
        // No-op
    }
}

/**
 * In-memory user cache. Could be expanded to use SharedPreferences or a different cache.
 */
class CacheWalletDataSource : WalletDataSource {
    private var address: String? = null

    override suspend fun getWalletAddress(): Either<Failure, String?> {
        return address?.let {
            Either.Right(it)
        } ?: Either.Left(DataError.CacheMissError)
    }

    override suspend fun setWalletAddress(address: String): Either<Failure, Unit> {
        this.address = address
        return Either.Right(Unit)
    }

    override suspend fun clear() {
        this.address = null
    }
}
