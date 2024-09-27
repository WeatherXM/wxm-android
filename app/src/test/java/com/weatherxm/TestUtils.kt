package com.weatherxm

import arrow.core.Either
import com.haroldadmin.cnradapter.NetworkResponse
import com.weatherxm.TestConfig.failure
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.NetworkError
import com.weatherxm.data.network.ErrorResponse
import com.weatherxm.ui.common.Charts
import com.weatherxm.ui.common.LineChartData
import com.weatherxm.ui.common.Resource
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.scopes.BehaviorSpecWhenContainerScope
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.every
import net.bytebuddy.utility.RandomString
import retrofit2.Response
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.net.SocketTimeoutException

object TestUtils {
    fun <T : Any> Either<Failure, T?>.isSuccess(successData: T?) {
        this shouldBe Either.Right(successData)
    }

    fun <T : Any> Either<Failure, T?>.isError() {
        this shouldBe Either.Left(failure)
    }

    fun <T : Any> Resource<T>?.isSuccess(data: T?) {
        this shouldBe Resource.success(data)
    }

    fun <T : Any> Resource<T>?.isError(errorMsg: String) {
        this shouldBe Resource.error(errorMsg)
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

    /**
     * https://stackoverflow.com/a/76813421/5403137
     *
     * Reflection methods to access private fields
     */
    @Suppress("NestedBlockDepth")
    private fun getModifiersField(): Field {
        return try {
            Field::class.java.getDeclaredField("modifiers")
        } catch (e: NoSuchFieldException) {
            try {
                val getDeclaredFields0: Method =
                    Class::class.java.getDeclaredMethod(
                        "getDeclaredFields0",
                        Boolean::class.javaPrimitiveType
                    )
                getDeclaredFields0.isAccessible = true
                val fields = getDeclaredFields0.invoke(Field::class.java, false) as Array<Field>
                for (field in fields) {
                    if ("modifiers" == field.name) {
                        return field
                    }
                }
            } catch (ex: ReflectiveOperationException) {
                e.addSuppressed(ex)
            }
            throw e
        }
    }

    fun setStaticFieldViaReflection(field: Field, value: Any) {
        field.isAccessible = true
        getModifiersField().also {
            it.isAccessible = true
            it.set(field, field.modifiers and Modifier.FINAL.inv())
        }
        field.set(null, value)
    }
}
