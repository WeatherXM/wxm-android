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
        return userRepository.getProfileInfo()
    }

    override fun getWalletAddressFromCache(): String? {
        return userRepository.getWalletAddress()
    }
}
