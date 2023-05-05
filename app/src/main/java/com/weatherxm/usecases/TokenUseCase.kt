package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.repository.TokenRepository
import com.weatherxm.ui.token.UITransaction
import com.weatherxm.ui.token.UITransactions
import com.weatherxm.util.Mask
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

interface TokenUseCase {
    suspend fun getTransactions(
        deviceId: String,
        page: Int?,
        fromDate: String?,
        toDate: String? = null
    ): Either<Failure, UITransactions>
}

class TokenUseCaseImpl(
    private val tokenRepository: TokenRepository
) : TokenUseCase {
    private val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    private val timestampFormat = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)

    override suspend fun getTransactions(
        deviceId: String,
        page: Int?,
        fromDate: String?,
        toDate: String?
    ): Either<Failure, UITransactions> {
        return tokenRepository.getTransactions(
            deviceId = deviceId,
            page = page,
            fromDate = fromDate,
            toDate = toDate
        ).map {
            if (it.data.isEmpty()) {
                UITransactions(listOf(), reachedTotal = true)
            } else {
                val uiTransactions = it.data
                    .filter { tx ->
                        // Keep only transactions that have a reward for this device
                        tx.actualReward != null
                    }
                    .map { tx ->
                        UITransaction(
                            formattedDate = tx.timestamp.format(dateFormat),
                            formattedTimestamp = tx.timestamp.format(timestampFormat),
                            txHash = tx.txHash,
                            txHashMasked = tx.txHash?.let { hash ->
                                Mask.maskHash(
                                    hash = hash,
                                    offsetStart = 8,
                                    offsetEnd = 8,
                                    maxMaskedChars = 6
                                )
                            },
                            validationScore = tx.validationScore,
                            dailyReward = tx.dailyReward,
                            actualReward = tx.actualReward
                        )
                    }
                UITransactions(uiTransactions, it.hasNextPage)
            }
        }
    }
}
