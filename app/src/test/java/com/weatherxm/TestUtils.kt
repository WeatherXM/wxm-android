package com.weatherxm

import arrow.core.Either
import com.weatherxm.data.ApiError
import com.weatherxm.data.Failure
import com.weatherxm.data.NetworkError
import com.weatherxm.data.Resource
import io.kotest.matchers.shouldBe
import net.bytebuddy.utility.RandomString
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier

object TestUtils {
    fun Failure.isDeviceNotFound() = this is ApiError.DeviceNotFound
    fun Failure.isNoConnectionError() = this is NetworkError.NoConnectionError

    fun <T : Any> Either<Failure, T?>.isSuccess(successData: T?) {
        this shouldBe Either.Right(successData)
    }

    fun <T : Any> Resource<T>?.isSuccess(data: T?) {
        this shouldBe Resource.success(data)
    }

    fun <T : Any> Resource<T>?.isError(errorMsg: String) {
        this shouldBe Resource.error(errorMsg)
    }

    fun createRandomString(length: Int): String {
        return RandomString.make(length)
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
