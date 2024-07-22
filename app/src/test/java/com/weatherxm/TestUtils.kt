package com.weatherxm

import arrow.core.Either
import com.weatherxm.data.ApiError
import com.weatherxm.data.Failure
import com.weatherxm.data.NetworkError
import com.weatherxm.data.Resource
import io.kotest.matchers.shouldBe
import net.bytebuddy.utility.RandomString

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
}
