package com.weatherxm

import androidx.lifecycle.LiveData
import arrow.core.Either
import com.haroldadmin.cnradapter.NetworkResponse
import com.weatherxm.TestConfig.context
import com.weatherxm.TestConfig.failure
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.NetworkError
import com.weatherxm.data.network.ErrorResponse
import com.weatherxm.ui.common.Charts
import com.weatherxm.ui.common.LineChartData
import com.weatherxm.ui.common.Resource
import com.weatherxm.ui.common.WeatherUnit
import com.weatherxm.ui.common.WeatherUnitType
import com.weatherxm.util.UnitSelector
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.scopes.BehaviorSpecWhenContainerScope
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import net.bytebuddy.utility.RandomString
import retrofit2.Response
import java.net.SocketTimeoutException

object TestUtils {
    fun defaultMockUnitSelector() {
        every { UnitSelector.getTemperatureUnit(context) } returns WeatherUnit(
            WeatherUnitType.CELSIUS,
            context.getString(R.string.temperature_celsius)
        )
        every { UnitSelector.getPrecipitationUnit(context, false) } returns WeatherUnit(
            WeatherUnitType.MILLIMETERS,
            context.getString(R.string.precipitation_mm)
        )
        every { UnitSelector.getPrecipitationUnit(context, true) } returns WeatherUnit(
            WeatherUnitType.MILLIMETERS,
            context.getString(R.string.precipitation_mm_hour)
        )
        every { UnitSelector.getWindUnit(context) } returns WeatherUnit(
            WeatherUnitType.MS,
            context.getString(R.string.wind_speed_ms)
        )
        every { UnitSelector.getWindDirectionUnit(context) } returns WeatherUnit(
            WeatherUnitType.CARDINAL,
            context.getString(R.string.wind_direction_cardinal)
        )
        every { UnitSelector.getPressureUnit(context) } returns WeatherUnit(
            WeatherUnitType.HPA,
            context.getString(R.string.pressure_hpa)
        )
    }

    fun <T : Any> Either<Failure, T?>.isSuccess(successData: T?) {
        this shouldBe Either.Right(successData)
    }

    fun <T : Any> Either<Failure, T?>.isError() {
        this shouldBe Either.Left(failure)
    }

    fun <T : Any> LiveData<Resource<T>>?.isSuccess(data: T?) {
        this?.value shouldBe Resource.success(data)
    }

    fun <T : Any> LiveData<Resource<T>>?.isError(errorMsg: String) {
        this?.value shouldBe Resource.error(errorMsg)
    }

    fun mockEitherLeft(function: () -> Either<Failure, Any?>, failure: Failure) {
        every { function() } returns Either.Left(failure)
    }

    fun coMockEitherLeft(function: suspend () -> Either<Failure, Any?>, failure: Failure) {
        coEvery { function() } returns Either.Left(failure)
    }

    fun mockEitherRight(function: () -> Either<Failure, Any?>, data: Any?) {
        every { function() } returns Either.Right(data)
    }

    fun coMockEitherRight(function: suspend () -> Either<Failure, Any?>, data: Any?) {
        coEvery { function() } returns Either.Right(data)
    }

    fun retrofitResponse(data: Any): Response<Any> {
        return Response.success(data)
    }

    suspend fun <T : Any> BehaviorSpecWhenContainerScope.testNetworkCall(
        dataTitle: String,
        data: Any? = null,
        successResponse: NetworkResponse<T, ErrorResponse>,
        mockFunction: suspend () -> NetworkResponse<T, ErrorResponse>,
        runFunction: suspend () -> Either<Failure, Any?>
    ) {
        and("the response is a success") {
            coEvery { mockFunction() } returns successResponse
            then("return $dataTitle") {
                runFunction().isSuccess(data)
            }
        }
        and("the response is a failure") {
            coEvery {
                mockFunction()
            } returns NetworkResponse.NetworkError(SocketTimeoutException())
            then("return the failure") {
                runFunction().leftOrNull().shouldBeInstanceOf<NetworkError>()
            }
        }
    }

    suspend fun BehaviorSpecWhenContainerScope.testGetFromCache(
        dataTitle: String,
        data: Any? = null,
        mockFunction: suspend () -> Either<Failure, Any?>,
        runFunction: suspend () -> Either<Failure, Any?>
    ) {
        and("the response is a success") {
            coMockEitherRight({ mockFunction() }, data)
            then("return the $dataTitle") {
                runFunction().isSuccess(data)
            }
        }
        and("the response is a failure") {
            coMockEitherLeft({ mockFunction() }, failure)
            then("return the failure") {
                runFunction().isError()
            }
        }
    }

    suspend fun BehaviorSpecWhenContainerScope.testThrowNotImplemented(
        function: suspend () -> Any?
    ) {
        then("Should throw a NotImplementedError") {
            shouldThrow<NotImplementedError> { function() }
        }
    }

    suspend fun <T : Any> BehaviorSpecWhenContainerScope.testHandleFailureViewModel(
        functionToRun: suspend () -> Any?,
        analytics: AnalyticsWrapper,
        liveDataToCheck: LiveData<Resource<T>>,
        verifyNumberOfFailureEvents: Int,
        error: String
    ) {
        runTest { functionToRun() }
        then("Log that error as a failure event") {
            verify(exactly = verifyNumberOfFailureEvents) { analytics.trackEventFailure(any()) }
        }
        then("LiveData posts an error with a specific $error message") {
            liveDataToCheck.isError(error)
        }
    }

    fun createRandomString(length: Int): String {
        return RandomString.make(length)
    }

    fun Charts.isEqual(other: Charts) {
        this.date shouldBe other.date
        this.temperature.isEqual(other.temperature)
        this.feelsLike.isEqual(other.feelsLike)
        this.precipitation.isEqual(other.precipitation)
        this.precipitationAccumulated.isEqual(other.precipitationAccumulated)
        this.precipProbability.isEqual(other.precipProbability)
        this.windSpeed.isEqual(other.windSpeed)
        this.windGust.isEqual(other.windGust)
        this.windDirection.isEqual(other.windDirection)
        this.humidity.isEqual(other.humidity)
        this.pressure.isEqual(other.pressure)
        this.uv.isEqual(other.uv)
        this.solarRadiation.isEqual(other.solarRadiation)
    }

    fun LineChartData.isEqual(other: LineChartData) {
        this.timestamps.forEachIndexed { i, item ->
            item shouldBe other.timestamps[i]
        }
        this.entries.forEachIndexed { i, item ->
            item.x shouldBe other.entries[i].x
            item.y shouldBe other.entries[i].y
        }
    }
}
