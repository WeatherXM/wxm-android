package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.User
import com.weatherxm.data.repository.UserRepository
import com.weatherxm.ui.ProfileInfo
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface UserUseCase {
    suspend fun getUser(): Either<Failure, User>
    suspend fun getProfileInfo(): Either<Failure, ProfileInfo>
    fun getWalletAddressFromCache(): String?
}

class UserUseCaseImpl : UserUseCase, KoinComponent {
    private val userRepository: UserRepository by inject()

    override suspend fun getUser(): Either<Failure, User> {
        return userRepository.getUser()
    }

    override suspend fun getProfileInfo(): Either<Failure, ProfileInfo> {
        val profileInfo = ProfileInfo()

        return if (userRepository.hasDataInCache()) {
            profileInfo.email = userRepository.getEmail()
            profileInfo.name = userRepository.getName()
            profileInfo.walletAddress = userRepository.getWalletAddress()
            Either.Right(profileInfo)
        } else {
            userRepository.getUser()
                .map {
                    profileInfo.email = it.email
                    profileInfo.name = it.name
                    profileInfo.walletAddress = it.wallet?.address
                    profileInfo
                }
        }
    }

    override fun getWalletAddressFromCache(): String? {
        return userRepository.getWalletAddress()
    }
}
