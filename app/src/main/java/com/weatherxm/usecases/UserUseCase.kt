package com.weatherxm.usecases

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import com.weatherxm.data.DataError
import com.weatherxm.data.Failure
import com.weatherxm.data.User
import com.weatherxm.data.repository.UserPreferencesRepository
import com.weatherxm.data.repository.UserRepository
import com.weatherxm.data.repository.WalletRepository
import java.util.concurrent.TimeUnit

interface UserUseCase {
    suspend fun getUser(): Either<Failure, User>
    suspend fun getWalletAddress(): Either<Failure, String>
    suspend fun shouldShowWalletMissingWarning(): Boolean
    fun setWalletWarningDismissTimestamp()
}

class UserUseCaseImpl(
    private val userRepository: UserRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val walletRepository: WalletRepository
) : UserUseCase {
    companion object {
        val WALLET_WARNING_DISMISS_EXPIRATION = TimeUnit.HOURS.toMillis(24L)
    }

    override suspend fun getUser(): Either<Failure, User> {
        return userRepository.getUser()
    }

    override suspend fun getWalletAddress(): Either<Failure, String> {
        return walletRepository.getWalletAddress()
            .flatMap { it?.right() ?: DataError.NoWalletAddressError.left<Failure>() }
    }

    override suspend fun shouldShowWalletMissingWarning(): Boolean {
        val hasWalletAddress = walletRepository.getWalletAddress()
            .flatMap { b -> b?.right() ?: DataError.NoWalletAddressError.left<Failure>() }
            .fold({ it !is DataError.NoWalletAddressError }, { it.isNotEmpty() })

        val dismissTimestamp = userPreferencesRepository.getWalletWarningDismissTimestamp()
        val now = System.currentTimeMillis()

        return !hasWalletAddress && (now - dismissTimestamp) > WALLET_WARNING_DISMISS_EXPIRATION
    }

    override fun setWalletWarningDismissTimestamp() {
        userPreferencesRepository.setWalletWarningDismissTimestamp()
    }
}
