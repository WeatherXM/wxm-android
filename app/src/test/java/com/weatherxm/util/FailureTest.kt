package com.weatherxm.util

import com.weatherxm.R
import com.weatherxm.TestConfig.resources
import com.weatherxm.data.models.ApiError.GenericError.UnknownError
import com.weatherxm.data.models.ApiError.GenericError.UnsupportedAppVersion
import com.weatherxm.data.models.ApiError.GenericError.ValidationError
import com.weatherxm.data.models.Failure
import com.weatherxm.data.models.NetworkError
import com.weatherxm.util.Failure.getCode
import com.weatherxm.util.Failure.getDefaultMessage
import com.weatherxm.util.Failure.getDefaultMessageResId
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class FailureTest : BehaviorSpec({
    beforeSpec {
        startKoin {
            modules(
                module {
                    single<Resources> {
                        resources
                    }
                }
            )
        }
        every { resources.getString(R.string.error_unsupported_error) } returns "Error Unsupported"
    }

    context("Map Failure to UI messages") {
        given("UnsupportedAppVersion") {
            val unsupportedAppVersion = UnsupportedAppVersion("")
            unsupportedAppVersion.getDefaultMessageResId() shouldBe R.string.error_unsupported_error
            unsupportedAppVersion.getDefaultMessage() shouldBe "Error Unsupported"

        }
        given("NoConnectionError") {
            then("should return R.string.error_network_generic") {
                NetworkError.NoConnectionError()
                    .getDefaultMessageResId() shouldBe R.string.error_network_generic
            }
        }
        given("ConnectionTimeoutError") {
            then("should return R.string.error_network_timed_out") {
                NetworkError.ConnectionTimeoutError()
                    .getDefaultMessageResId() shouldBe R.string.error_network_timed_out
            }
        }
        given("ValidationError") {
            then("should return R.string.error_server_validation") {
                ValidationError("")
                    .getDefaultMessageResId() shouldBe R.string.error_server_validation
            }
        }
        given("Any other error") {
            When("There is a fallback value") {
                then("should return the fallback value") {
                    UnknownError()
                        .getDefaultMessageResId(
                            R.string.error_reach_out_short
                        ) shouldBe R.string.error_reach_out_short
                }
            }
            When("There is no fallback value") {
                then("should return R.string.error_reach_out") {
                    UnknownError().getDefaultMessageResId() shouldBe R.string.error_reach_out
                }
            }
        }
    }

    context("Get Failure Code") {
        given("an error having a code such as NoConnectionError") {
            then("should return Failure.CODE_NO_CONNECTION") {
                NetworkError.NoConnectionError().getCode() shouldBe Failure.CODE_NO_CONNECTION
            }
        }
        given("an error having not having a code") {
            then("should return Failure.CODE_UNKNOWN") {
                UnsupportedAppVersion(null).getCode() shouldBe Failure.CODE_UNKNOWN
            }
        }
    }

    afterSpec {
        stopKoin()
    }
})
