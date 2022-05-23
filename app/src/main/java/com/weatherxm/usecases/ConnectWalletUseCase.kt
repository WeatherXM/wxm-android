package com.weatherxm.usecases

import arrow.core.Either
import arrow.core.leftIfNull
import com.weatherxm.data.DataError
import com.weatherxm.data.Failure
import com.weatherxm.data.repository.WalletRepository

interface ConnectWalletUseCase {
    suspend fun getWalletAddress(): Either<Failure, String>
    suspend fun setWalletAddress(address: String): Either<Failure, Unit>
}

class ConnectWalletUseCaseImpl(
    private val walletRepository: WalletRepository
) : ConnectWalletUseCase {

    override suspend fun getWalletAddress(): Either<Failure, String> {
        return walletRepository.getWalletAddress()
            .leftIfNull { DataError.NoWalletAddressError }
    }

    override suspend fun setWalletAddress(address: String): Either<Failure, Unit> {
        return walletRepository.setWalletAddress(address)
    }
}
