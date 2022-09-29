package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.data.Failure
import com.weatherxm.data.LastAndDatedTxs
import com.weatherxm.data.Transaction
import com.weatherxm.data.Transaction.Companion.VERY_SMALL_NUMBER_FOR_CHART
import com.weatherxm.data.TransactionsResponse
import com.weatherxm.data.datasource.TokenDataSource
import com.weatherxm.util.DateTimeHelper.dateToLocalDate
import com.weatherxm.util.Tokens.roundTokens
import java.time.LocalDate

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
    ): Either<Failure, LastAndDatedTxs>

    suspend fun getAllPublicTransactionsInRange(
        deviceId: String,
        timezone: String?,
        fromDate: String?,
        toDate: String? = null
    ): Either<Failure, LastAndDatedTxs>
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
    ): Either<Failure, LastAndDatedTxs> {
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
    ): Either<Failure, LastAndDatedTxs> {
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
    ): Either<Failure, LastAndDatedTxs> {
        val resp =
            tokenDataSource.getTransactions(deviceId, page, pageSize, timezone, fromDate, toDate)
        resp
            .mapLeft {
                return Either.Left(it)
            }
            .map {
                txs.addAll(it.data)
                if (it.hasNextPage) {
                    val newPage = if (page == null) 1 else page + 1

                    // Keep getting the TXs recursively
                    getTxsRecursively(txs, deviceId, newPage, pageSize, timezone, fromDate, toDate)
                }
            }

        val lastReward = if (txs.isNotEmpty()) txs[0] else null
        val datedTxs = createDatedTransactionsList(dateToLocalDate(fromDate), txs)
        return Either.Right(LastAndDatedTxs(lastReward, datedTxs))
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
    ): Either<Failure, LastAndDatedTxs> {
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
                    getPublicTxsRecursively(
                        txs,
                        deviceId,
                        newPage,
                        pageSize,
                        timezone,
                        fromDate,
                        toDate
                    )
                }
            }

        val lastReward = if (txs.isNotEmpty()) txs[0] else null
        val datedTxs = createDatedTransactionsList(dateToLocalDate(fromDate), txs)
        return Either.Right(LastAndDatedTxs(lastReward, datedTxs))
    }

    private fun createDatedTransactionsList(
        fromDate: LocalDate?,
        transactions: List<Transaction>
    ): List<Pair<String, Float>> {
        val datesAndTxs = mutableMapOf<LocalDate, Float>()
        val lastMonthDates = mutableListOf<LocalDate>()
        var nowDate = LocalDate.now()

        // Create a list of dates, and a map of dates and transactions from latest -> earliest
        while (!nowDate.isBefore(fromDate)) {
            lastMonthDates.add(nowDate)
            datesAndTxs[nowDate] = VERY_SMALL_NUMBER_FOR_CHART
            nowDate = nowDate.minusDays(1)
        }

        transactions
            .filter { it.actualReward != null && it.actualReward > 0.0F }
            .forEach { tx ->
                tx.actualReward?.let {
                    val date = tx.timestamp.toLocalDate()
                    val amountForDate = datesAndTxs.getOrDefault(date, 0.0F)

                    /*
                    * We need to round the tokens number
                    * as we use it further for getting the max in a range
                    * And we show that max rounded. Small differences occur if we don't round it.
                    * example: https://github.com/WeatherXM/issue-tracker/issues/97
                    */
                    datesAndTxs[date] = amountForDate + roundTokens(it)
                }
            }

        val datedTransactions = lastMonthDates.map {
            Pair(it.toString(), datesAndTxs.getOrDefault(it, VERY_SMALL_NUMBER_FOR_CHART))
        }

        return datedTransactions
    }
}
