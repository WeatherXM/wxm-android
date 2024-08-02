package com.weatherxm.util

import com.weatherxm.R
import com.weatherxm.data.ApiError.GenericError.UnknownError
import com.weatherxm.data.ApiError.GenericError.UnsupportedAppVersion
import com.weatherxm.data.ApiError.GenericError.ValidationError
import com.weatherxm.data.Failure
import com.weatherxm.data.NetworkError
import com.weatherxm.util.Failure.getCode
import com.weatherxm.util.Failure.getDefaultMessageResId
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class FailureTest : BehaviorSpec({
    context("Map Failure to UI messages") {
        given("UnsupportedAppVersion") {
            UnsupportedAppVersion("").getDefaultMessageResId() shouldBe
                R.string.error_unsupported_error
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
})
