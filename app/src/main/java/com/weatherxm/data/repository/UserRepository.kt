package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.User
import com.weatherxm.data.datasource.CacheUserDataSource
import com.weatherxm.data.datasource.NetworkUserDataSource
import com.weatherxm.ui.ProfileInfo
import org.koin.core.component.KoinComponent

class UserRepository(
    private val networkUserDataSource: NetworkUserDataSource,
    private val cacheUserDataSource: CacheUserDataSource
) : KoinComponent {

    suspend fun getUser(): Either<Failure, User> {
        return networkUserDataSource.getUser()
            .map {
                cacheUserDataSource.setEmail(it.email)
                cacheUserDataSource.setName(it.name)
                cacheUserDataSource.setWalletAddress(it.wallet?.address)
                it
            }
    }

    fun getWalletAddress(): String? {
        return cacheUserDataSource.getWalletAddress()
    }

    suspend fun saveAddress(address: String): Either<Failure, Unit> {
        cacheUserDataSource.setWalletAddress(address)
        return networkUserDataSource.saveAddress(address)
    }

    suspend fun getProfileInfo(): Either<Failure, ProfileInfo>  {
        val profileInfo = ProfileInfo()

        return if (cacheUserDataSource.hasDataInCache()) {
            profileInfo.email = cacheUserDataSource.getEmail()
            profileInfo.name = cacheUserDataSource.getName()
            profileInfo.walletAddress = cacheUserDataSource.getWalletAddress()
            Either.Right(profileInfo)
        } else {
            getUser()
                .map {
                    profileInfo.email = it.email
                    profileInfo.name = it.name
                    profileInfo.walletAddress = it.wallet?.address
                    profileInfo
                }
        }
    }
}
