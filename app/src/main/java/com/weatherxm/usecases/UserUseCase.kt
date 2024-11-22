package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.User
import com.weatherxm.data.repository.RewardsRepository
import com.weatherxm.data.repository.UserPreferencesRepository
import com.weatherxm.data.repository.UserRepository
import com.weatherxm.data.repository.WalletRepository
import com.weatherxm.ui.common.UIWalletRewards
import com.weatherxm.ui.common.empty
import java.util.concurrent.TimeUnit

interface UserUseCase {
    suspend fun getUser(forceRefresh: Boolean = false): Either<Failure, User>
    suspend fun getWalletAddress(): Either<Failure, String>
    suspend fun setWalletAddress(address: String): Either<Failure, Unit>
    fun shouldShowWalletMissingWarning(walletAddress: String): Boolean
    fun setWalletWarningDismissTimestamp()
    suspend fun getWalletRewards(walletAddress: String?): Either<Failure, UIWalletRewards>
    fun getUserId(): String
    fun shouldShowAnalyticsOptIn(): Boolean
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
        return walletRepository.getWalletAddress().map { it ?: String.empty() }
    }

    override suspend fun setWalletAddress(address: String): Either<Failure, Unit> {
        return walletRepository.setWalletAddress(address)
    }

    override fun shouldShowWalletMissingWarning(walletAddress: String): Boolean {
        val dismissTimestamp = userPreferencesRepository.getWalletWarningDismissTimestamp()
        val now = System.currentTimeMillis()
        return walletAddress.isEmpty() && now - dismissTimestamp > WALLET_WARNING_DISMISS_EXPIRATION
    }

    override fun setWalletWarningDismissTimestamp() {
        userPreferencesRepository.setWalletWarningDismissTimestamp()
    }

    override suspend fun getWalletRewards(
        walletAddress: String?
    ): Either<Failure, UIWalletRewards> {
        return if (walletAddress.isNullOrEmpty()) {
            Either.Right(UIWalletRewards.empty())
        } else {
            rewardsRepository.getWalletRewards(walletAddress).map { rewards ->
                UIWalletRewards(
                    rewards.cumulativeAmount ?: 0.0,
                    rewards.totalClaimed ?: 0.0,
                    rewards.available ?: 0.0,
                    walletAddress
                )
            }
        }

    }

    override fun getUserId(): String {
        return userRepository.getUserId()
    }

    override fun shouldShowAnalyticsOptIn(): Boolean {
        return userPreferencesRepository.shouldShowAnalyticsOptIn()
    }
}
