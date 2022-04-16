package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.datasource.CacheWalletDataSource
import com.weatherxm.data.datasource.NetworkWalletDataSource
import org.koin.core.component.KoinComponent
import timber.log.Timber

class WalletRepository(
    private val networkWalletDataSource: NetworkWalletDataSource,
    private val cacheWalletDataSource: CacheWalletDataSource
) : KoinComponent {

    /**
     * Gets wallet from cache or network, combining the underlying data sources
     */
    suspend fun getWalletAddress(): Either<Failure, String?> {
        return cacheWalletDataSource.getWalletAddress()
            .tap {
                Timber.d("Got wallet from cache [$it].")
            }
            .mapLeft {
                return networkWalletDataSource.getWalletAddress().tap { address ->
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
    suspend fun setWalletAddress(address: String): Either<Failure, Unit> {
        return networkWalletDataSource.setWalletAddress(address)
            .tap {
                Timber.d("Saved new wallet address [$address].")
                // Save also in cache, if network operation was successful
                cacheWalletDataSource.setWalletAddress(address)
            }
    }

    suspend fun clearCache() {
        cacheWalletDataSource.clear()
    }
}
