package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.User
import com.weatherxm.data.datasource.CacheUserDataSource
import com.weatherxm.data.datasource.NetworkUserDataSource
import org.koin.core.component.KoinComponent
import timber.log.Timber

class UserRepository(
    private val networkUserDataSource: NetworkUserDataSource,
    private val cacheUserDataSource: CacheUserDataSource
) : KoinComponent {

    /**
     * Gets user from cache or network, combining the underlying data sources
     */
    suspend fun getUser(): Either<Failure, User> {
        return cacheUserDataSource.getUser()
            .tap {
                Timber.d("Got user from cache [${it.email}].")
            }
            .mapLeft {
                return networkUserDataSource.getUser().tap {
                    Timber.d("Got user from network [${it.email}].")
                    cacheUserDataSource.setUser(it)
                }
            }
    }
}
