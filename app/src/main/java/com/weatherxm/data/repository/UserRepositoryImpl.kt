package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.User
import com.weatherxm.data.datasource.CacheUserDataSource
import com.weatherxm.data.datasource.NetworkUserDataSource
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import timber.log.Timber

class UserRepositoryImpl(
    private val networkUserDataSource: NetworkUserDataSource,
    private val cacheUserDataSource: CacheUserDataSource
) : UserRepository {

    /**
     * Gets user from cache or network, combining the underlying data sources
     */
    override suspend fun getUser(): Either<Failure, User> {
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

    override suspend fun getUserUsername(): Either<Failure, String> {
        return cacheUserDataSource.getUserUsername()
    }

    override fun getUserId(): String {
        return cacheUserDataSource.getUserId()
    }

    override suspend fun deleteAccount(): Either<Failure, Unit> {
        return withContext(NonCancellable) {
            networkUserDataSource.deleteAccount()
        }
    }

    override fun hasDismissedSurveyPrompt(): Boolean {
        return cacheUserDataSource.hasDismissedSurveyPrompt()
    }

    override fun dismissSurveyPrompt() {
        cacheUserDataSource.dismissSurveyPrompt()
    }

    override fun setWalletWarningDismissTimestamp() {
        cacheUserDataSource.setWalletWarningDismissTimestamp()
    }

    override fun getWalletWarningDismissTimestamp(): Long {
        return cacheUserDataSource.getWalletWarningDismissTimestamp()
    }
}
