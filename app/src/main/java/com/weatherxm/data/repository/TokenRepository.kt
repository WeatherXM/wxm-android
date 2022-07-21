package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.Transaction
import com.weatherxm.data.TransactionsResponse
import com.weatherxm.data.datasource.TokenDataSource

interface TokenRepository {
    suspend fun getTransactions(
        deviceId: String,
        page: Int?,
        timezone: String?,
        fromDate: String?,
        toDate: String? = null
    ): Either<Failure, TransactionsResponse>

    suspend fun getAllTransactionsInRange(
        deviceId: String,
        timezone: String?,
        fromDate: String?,
        toDate: String? = null
    ): Either<Failure, List<Transaction>>

    suspend fun getAllPublicTransactionsInRange(
        deviceId: String,
        timezone: String?,
        fromDate: String?,
        toDate: String? = null
    ): Either<Failure, List<Transaction>>
}

class TokenRepositoryImpl(private val tokenDataSource: TokenDataSource) : TokenRepository {
    companion object {
        const val PAGE_SIZE_50 = 50
    }

    override suspend fun getTransactions(
        deviceId: String,
        page: Int?,
        timezone: String?,
        fromDate: String?,
        toDate: String?
    ): Either<Failure, TransactionsResponse> {
        return tokenDataSource.getTransactions(
            deviceId,
            page,
            timezone = timezone,
            fromDate = fromDate,
            toDate = toDate,
        )
    }

    override suspend fun getAllTransactionsInRange(
        deviceId: String,
        timezone: String?,
        fromDate: String?,
        toDate: String?
    ): Either<Failure, List<Transaction>> {
        val txs = mutableListOf<Transaction>()
        /*
        * The recursion should start from page = 0 until it reaches the last page
        * Also set a pageSize of 50 as the default is 10, and we want to fetch all the pages from
        * the API before sending the result back to the UI, so we increase the pageSize to not
        * spam the API and DDOS it (nor delay the UI also).
         */
        return getTxsRecursively(txs, deviceId, 0, PAGE_SIZE_50, timezone, fromDate, toDate)
    }

    override suspend fun getAllPublicTransactionsInRange(
        deviceId: String,
        timezone: String?,
        fromDate: String?,
        toDate: String?
    ): Either<Failure, List<Transaction>> {
        val txs = mutableListOf<Transaction>()
        /*
        * The recursion should start from page = 0 until it reaches the last page
        * Also set a pageSize of 50 as the default is 10, and we want to fetch all the pages from
        * the API before sending the result back to the UI, so we increase the pageSize to not
        * spam the API and DDOS it (nor delay the UI also).
         */
        return getPublicTxsRecursively(txs, deviceId, 0, PAGE_SIZE_50, timezone, fromDate, toDate)
    }

    @Suppress("LongParameterList")
    private suspend fun getTxsRecursively(
        txs: MutableList<Transaction>,
        deviceId: String,
        page: Int?,
        pageSize: Int?,
        timezone: String?,
        fromDate: String?,
        toDate: String?
    ): Either<Failure, List<Transaction>> {
        val resp =
            tokenDataSource.getTransactions(deviceId, page, pageSize, timezone, fromDate, toDate)
        resp
            .mapLeft {
                return Either.Left(it)
            }
            .map {
                txs.addAll(it.data)
                if (it.hasNextPage) {
                    val newPage = if (page == null) {
                        1
                    } else {
                        page + 1
                    }

                    // Keep getting the TXs recursively
                    getTxsRecursively(txs, deviceId, newPage, pageSize, timezone, fromDate, toDate)
                }
            }
        return Either.Right(txs)
    }

    @Suppress("LongParameterList")
    private suspend fun getPublicTxsRecursively(
        txs: MutableList<Transaction>,
        deviceId: String,
        page: Int?,
        pageSize: Int?,
        timezone: String?,
        fromDate: String?,
        toDate: String?
    ): Either<Failure, List<Transaction>> {
        val resp = tokenDataSource.getPublicTransactions(
            deviceId,
            page,
            pageSize,
            timezone,
            fromDate,
            toDate
        )
        resp
            .mapLeft {
                return Either.Left(it)
            }
            .map {
                txs.addAll(it.data)
                if (it.hasNextPage) {
                    val newPage = if (page == null) {
                        1
                    } else {
                        page + 1
                    }

                    // Keep getting the TXs recursively
                    getTxsRecursively(txs, deviceId, newPage, pageSize, timezone, fromDate, toDate)
                }
            }
        return Either.Right(txs)
    }
}
