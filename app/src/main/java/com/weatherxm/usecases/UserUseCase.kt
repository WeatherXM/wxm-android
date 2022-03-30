package com.weatherxm.usecases

import arrow.core.Either
import arrow.core.leftIfNull
import com.weatherxm.data.DataError
import com.weatherxm.data.Failure
import com.weatherxm.data.User
import com.weatherxm.data.repository.UserRepository
import com.weatherxm.data.repository.WalletRepository
import org.koin.core.component.KoinComponent

interface UserUseCase {
    suspend fun getUser(): Either<Failure, User>
    suspend fun getWalletAddress(): Either<Failure, String>
}

class UserUseCaseImpl(
    private val userRepository: UserRepository,
    private val walletRepository: WalletRepository
) : UserUseCase, KoinComponent {

    override suspend fun getUser(): Either<Failure, User> {
        return userRepository.getUser()
    }

    override suspend fun getWalletAddress(): Either<Failure, String> {
        return walletRepository.getWalletAddress()
            .leftIfNull { DataError.NoWalletAddressError }
    }
}
