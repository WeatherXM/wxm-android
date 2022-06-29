package com.weatherxm.usecases

import arrow.core.Either
import com.weatherxm.data.Device
import com.weatherxm.data.Failure
import com.weatherxm.data.HourlyWeather
import com.weatherxm.data.Transaction
import com.weatherxm.data.Transaction.Companion.VERY_SMALL_NUMBER_FOR_CHART
import com.weatherxm.data.UserActionError
import com.weatherxm.data.repository.DeviceRepository
import com.weatherxm.data.repository.TokenRepository
import com.weatherxm.data.repository.WeatherRepository
import com.weatherxm.ui.TokenInfo
import com.weatherxm.ui.TokenValuesChart
import com.weatherxm.util.DateTimeHelper.getFormattedDate
import com.weatherxm.util.DateTimeHelper.getLocalDate
import com.weatherxm.util.DateTimeHelper.getNowInTimezone
import com.weatherxm.util.DateTimeHelper.getTimezone
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.util.Date
import java.util.concurrent.TimeUnit

interface UserDeviceUseCase {
    suspend fun getUserDevices(): Either<Failure, List<Device>>
    suspend fun getUserDevice(deviceId: String): Either<Failure, Device>
    suspend fun getTodayAndTomorrowForecast(
        device: Device,
        forceRefresh: Boolean = false
    ): Either<Failure, List<HourlyWeather>>

    suspend fun getTokenInfoLast30D(deviceId: String): Either<Failure, TokenInfo>
    fun createDatedTransactionsList(
        fromDate: LocalDate,
        timezone: String,
        transactions: List<Transaction>
    ): List<Pair<String, Float>>

    suspend fun setFriendlyName(deviceId: String, friendlyName: String): Either<Failure, Unit>
    suspend fun clearFriendlyName(deviceId: String): Either<Failure, Unit>
    fun canChangeFriendlyName(deviceId: String): Either<UserActionError, Boolean>
}

class UserDeviceUseCaseImpl(
    private val deviceRepository: DeviceRepository,
    private val tokenRepository: TokenRepository,
    private val weatherRepository: WeatherRepository
) : UserDeviceUseCase {

    companion object {
        // Allow device friendly name change once in 10 minutes
        val FRIENDLY_NAME_TIME_LIMIT = TimeUnit.MINUTES.toMillis(10)
    }

    override suspend fun getUserDevices(): Either<Failure, List<Device>> {
        return deviceRepository.getUserDevices()
    }

    override suspend fun getUserDevice(deviceId: String): Either<Failure, Device> {
        return deviceRepository.getUserDevice(deviceId)
    }

    override fun createDatedTransactionsList(
        fromDate: LocalDate,
        timezone: String,
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

        transactions.forEach { tx ->
            val date = getLocalDate(tx.timestamp)

            val amountForDate = datesAndTxs.getOrDefault(date, 0.0F)
            if (tx.actualReward != null && tx.actualReward > 0.0F) {
                datesAndTxs[date] = amountForDate + tx.actualReward
            }
        }

        val datedTransactions = mutableListOf<Pair<String, Float>>()
        lastMonthDates.forEach {
            datedTransactions.add(
                Pair(it.toString(), datesAndTxs.getOrDefault(it, VERY_SMALL_NUMBER_FOR_CHART))
            )
        }

        return datedTransactions
    }

    // We suppress magic number because we use specific numbers to check last month and last week
    @Suppress("MagicNumber")
    override suspend fun getTokenInfoLast30D(deviceId: String): Either<Failure, TokenInfo> {
        val now = getNowInTimezone()
        val fromDateAsLocalDate = getLocalDate(now.minusDays(30).toString())
        val formattedFromDate = fromDateAsLocalDate.toString()
        val timezone = getTimezone()

        return tokenRepository.getAllTransactionsInRange(deviceId, timezone, formattedFromDate)
            .map { transactions ->
                if (transactions.isNotEmpty()) {
                    val lastReward = transactions[0]
                    var total7d: Float? = 0.0F
                    var total30d: Float? = 0.0F
                    val chart7d = TokenValuesChart(mutableListOf())
                    val chart30d = TokenValuesChart(mutableListOf())
                    val datedTransactions =
                        createDatedTransactionsList(fromDateAsLocalDate, timezone, transactions)

                    /*
                    * Populate the totals and the chart data from latest -> earliest
                     */
                    for ((position, datedTx) in datedTransactions.withIndex()) {
                        if (position <= 6) {
                            if (datedTx.second > VERY_SMALL_NUMBER_FOR_CHART) {
                                total7d = total7d?.plus(datedTx.second)
                            }
                            chart7d.values.add(datedTx)
                        }
                        if (datedTx.second > VERY_SMALL_NUMBER_FOR_CHART) {
                            total30d = total30d?.plus(datedTx.second)
                        }
                        chart30d.values.add(datedTx)
                    }

                    // Find the maximum 7 and 30 day rewards (AKA the biggest bar on the chart)
                    val max7dReward = chart7d.values.maxOfOrNull { it.second }
                    val max30dReward = chart30d.values.maxOfOrNull { it.second }

                    /*
                    * We need to reverse the order in the chart data because we have saved them
                    * from latest -> earliest but we need the earliest -> latest for proper
                    * displaying them
                    */
                    chart7d.values = chart7d.values.reversed().toMutableList()
                    chart30d.values = chart30d.values.reversed().toMutableList()

                    TokenInfo(
                        lastReward,
                        total7d,
                        chart7d,
                        max7dReward,
                        total30d,
                        chart30d,
                        max30dReward
                    )
                } else {
                    TokenInfo()
                }
            }
    }

    override suspend fun getTodayAndTomorrowForecast(
        device: Device,
        forceRefresh: Boolean
    ): Either<Failure, List<HourlyWeather>> {
        val now = getNowInTimezone(device.timezone)
        val today = getFormattedDate(now)
        val tomorrow = getFormattedDate(now.plusDays(1))

        return weatherRepository.getDeviceForecast(device.id, now, now.plusDays(1), forceRefresh)
            .map { response ->
                val hourlyForecastToReturn = mutableListOf<HourlyWeather>()
                hourlyForecastToReturn.apply {
                    response.forEach {
                        if (it.date.equals(today) || it.date.equals(tomorrow)) {
                            it.hourly?.let { hourlyForecast ->
                                this.addAll(hourlyForecast)
                            }
                        }
                    }
                }
            }
    }

    override suspend fun setFriendlyName(
        deviceId: String,
        friendlyName: String
    ): Either<Failure, Unit> {
        return deviceRepository.setFriendlyName(deviceId, friendlyName)
    }

    override suspend fun clearFriendlyName(deviceId: String): Either<Failure, Unit> {
        return deviceRepository.clearFriendlyName(deviceId)
    }

    override fun canChangeFriendlyName(deviceId: String): Either<UserActionError, Boolean> {
        // Check if user has already set a friendly name within a predefined time window
        val lastFriendlyNameChanged = runBlocking {
            deviceRepository.getLastFriendlyNameChanged(deviceId)
        }
        val diff = Date().time - lastFriendlyNameChanged
        return if (diff >= FRIENDLY_NAME_TIME_LIMIT) {
            Either.Right(true)
        } else {
            Either.Left(
                UserActionError.UserActionRateLimitedError(
                    "${diff}ms passed since last name change [Limit ${FRIENDLY_NAME_TIME_LIMIT}ms]"
                )
            )
        }
    }
}
