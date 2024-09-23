package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.data.models.Failure
import com.weatherxm.data.datasource.CacheWalletDataSource
import com.weatherxm.data.datasource.NetworkWalletDataSource
import timber.log.Timber

interface WalletRepository {
    suspend fun getWalletAddress(): Either<Failure, String?>
    suspend fun setWalletAddress(address: String): Either<Failure, Unit>
}

class WalletRepositoryImpl(
    private val networkWalletDataSource: NetworkWalletDataSource,
    private val cacheWalletDataSource: CacheWalletDataSource
) : WalletRepository {

    /**
     * Gets wallet from cache or network, combining the underlying data sources
     */
    override suspend fun getWalletAddress(): Either<Failure, String?> {
        return cacheWalletDataSource.getWalletAddress()
            .onRight {
                Timber.d("Got wallet from cache [$it].")
            }
            .mapLeft {
                return networkWalletDataSource.getWalletAddress().onRight { address ->
                    Timber.d("Got wallet from network [$address].")
                    address?.let {
                        cacheWalletDataSource.setWalletAddress(it)
                    }
                }
            }
    }

    /**
     * Save wallet address
     */
    override suspend fun setWalletAddress(address: String): Either<Failure, Unit> {
        return networkWalletDataSource.setWalletAddress(address)
            .onRight {
                Timber.d("Saved new wallet address [$address].")
                // Save also in cache, if network operation was successful
                cacheWalletDataSource.setWalletAddress(address)
            }
    }
}
