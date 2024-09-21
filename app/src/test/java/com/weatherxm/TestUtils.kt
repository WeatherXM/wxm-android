package com.weatherxm

import arrow.core.Either
import com.weatherxm.TestConfig.failure
import com.weatherxm.data.models.Failure
import com.weatherxm.ui.common.Resource
import com.weatherxm.ui.common.Charts
import com.weatherxm.ui.common.LineChartData
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import net.bytebuddy.utility.RandomString
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier

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
