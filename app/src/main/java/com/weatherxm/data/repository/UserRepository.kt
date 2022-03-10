package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.User
import com.weatherxm.data.datasource.CacheUserDataSource
import com.weatherxm.data.datasource.UserDataSource
import org.koin.core.component.KoinComponent

class UserRepository(
    private val userDataSource: UserDataSource,
    private val cacheUserDataSource: CacheUserDataSource
) : KoinComponent {

    suspend fun getUser(): Either<Failure, User> {
        return userDataSource.getUser()
            .map {
                cacheUserDataSource.setEmail(it.email)
                cacheUserDataSource.setName(it.name)
                cacheUserDataSource.setWalletAddress(it.wallet?.address)
                it
            }
    }

    fun getEmail(): String {
        return cacheUserDataSource.getEmail()
    }

    fun getName(): String? {
        return cacheUserDataSource.getName()
    }

    fun getWalletAddress(): String? {
        return cacheUserDataSource.getWalletAddress()
    }

    fun hasDataInCache(): Boolean {
        return cacheUserDataSource.hasDataInCache()
    }

    suspend fun saveAddress(address: String): Either<Failure, Unit> {
        cacheUserDataSource.setWalletAddress(address)
        return userDataSource.saveAddress(address)
    }
}
