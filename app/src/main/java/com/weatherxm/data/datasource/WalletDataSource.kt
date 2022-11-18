package com.weatherxm.data.datasource

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.map
import com.weatherxm.data.network.AddressBody
import com.weatherxm.data.network.ApiService
import com.weatherxm.data.services.CacheService

interface WalletDataSource {
    suspend fun getWalletAddress(): Either<Failure, String?>
    suspend fun setWalletAddress(address: String): Either<Failure, Unit>
}

class NetworkWalletDataSource(private val apiService: ApiService) : WalletDataSource {

    override suspend fun getWalletAddress(): Either<Failure, String?> {
        return apiService.getWallet().map().map { it.address }
    }

    override suspend fun setWalletAddress(address: String): Either<Failure, Unit> {
        return apiService.setWallet(AddressBody(address)).map()
    }
}

class CacheWalletDataSource(private val cacheService: CacheService) : WalletDataSource {
    override suspend fun getWalletAddress(): Either<Failure, String?> {
        return cacheService.getWalletAddress()
    }

    override suspend fun setWalletAddress(address: String): Either<Failure, Unit> {
        cacheService.setWalletAddress(address)
        return Either.Right(Unit)
    }
}
