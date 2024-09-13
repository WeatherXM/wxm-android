package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.repository.WalletRepository
import com.weatherxm.ui.common.empty

interface WalletUseCase {
    suspend fun getWalletAddress(): Either<Failure, String>
    suspend fun setWalletAddress(address: String): Either<Failure, Unit>
}

class WalletUseCaseImpl(
    private val walletRepository: WalletRepository
) : WalletUseCase {

    override suspend fun getWalletAddress(): Either<Failure, String> {
        return walletRepository.getWalletAddress().map { it ?: String.empty() }
    }

    override suspend fun setWalletAddress(address: String): Either<Failure, Unit> {
        return walletRepository.setWalletAddress(address)
    }
}
