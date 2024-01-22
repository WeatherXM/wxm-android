package com.weatherxm.usecases

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import com.weatherxm.data.DataError
import com.weatherxm.data.Failure
import com.weatherxm.data.User
import com.weatherxm.data.repository.RewardsRepository
import com.weatherxm.data.repository.UserPreferencesRepository
import com.weatherxm.data.repository.UserRepository
import com.weatherxm.data.repository.WalletRepository
import com.weatherxm.ui.common.UIWalletRewards
import java.math.BigInteger
import java.util.concurrent.TimeUnit

interface UserUseCase {
    suspend fun getUser(forceRefresh: Boolean = false): Either<Failure, User>
    suspend fun getWalletAddress(): Either<Failure, String>
    suspend fun shouldShowWalletMissingWarning(): Boolean
    fun setWalletWarningDismissTimestamp()
    suspend fun getWalletRewards(walletAddress: String?): Either<Failure, UIWalletRewards>
}

class UserUseCaseImpl(
    private val userRepository: UserRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val walletRepository: WalletRepository,
    private val rewardsRepository: RewardsRepository,
) : UserUseCase {
    companion object {
        val WALLET_WARNING_DISMISS_EXPIRATION = TimeUnit.HOURS.toMillis(24L)
    }

    override suspend fun getUser(forceRefresh: Boolean): Either<Failure, User> {
        return userRepository.getUser(forceRefresh)
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

        return !hasWalletAddress && now - dismissTimestamp > WALLET_WARNING_DISMISS_EXPIRATION
    }

    override fun setWalletWarningDismissTimestamp() {
        userPreferencesRepository.setWalletWarningDismissTimestamp()
    }

    override suspend fun getWalletRewards(
        walletAddress: String?
    ): Either<Failure, UIWalletRewards> {
        return if (walletAddress.isNullOrEmpty()) {
            Either.Right(
                UIWalletRewards(
                    BigInteger.ZERO,
                    BigInteger.ZERO,
                    BigInteger.ZERO,
                    ""
                )
            )
        } else {
            rewardsRepository.getWalletRewards(walletAddress).map { rewards ->
                UIWalletRewards(
                    rewards.cumulativeAmount ?: BigInteger.ZERO,
                    rewards.totalClaimed ?: BigInteger.ZERO,
                    rewards.available ?: BigInteger.ZERO,
                    walletAddress
                )
            }
        }

    }
}
