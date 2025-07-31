package com.weatherxm.data

import androidx.work.Constraints
import androidx.work.NetworkType
import arrow.core.Either
import com.haroldadmin.cnradapter.NetworkResponse
import com.weatherxm.data.models.ApiError
import com.weatherxm.data.models.ApiError.GenericError.JWTError.ForbiddenError
import com.weatherxm.data.models.ApiError.GenericError.JWTError.UnauthorizedError
import com.weatherxm.data.models.ApiError.GenericError.JWTError.UserNotFoundError
import com.weatherxm.data.models.ApiError.GenericError.NotFoundError
import com.weatherxm.data.models.ApiError.GenericError.UnknownError
import com.weatherxm.data.models.ApiError.GenericError.UnsupportedAppVersion
import com.weatherxm.data.models.ApiError.GenericError.ValidationError
import com.weatherxm.data.models.ApiError.UserError.WalletError.WalletAddressNotFound
import com.weatherxm.data.models.Location
import com.weatherxm.data.models.NetworkError
import com.weatherxm.data.network.ErrorResponse
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.net.SocketTimeoutException

class ExtensionsTest : BehaviorSpec({
    val path = "/api/v1/me/devices"
    val request = Request.Builder().url("https://api.weatherxm.com$path").build()
    val response =
        Response.Builder().request(request).code(200).protocol(Protocol.HTTP_2).message("").build()

    // [NetworkResponse] Success
    val body = "body"
    val retrofitResponse = retrofit2.Response.success(body)
    val successResponse = NetworkResponse.Success<String, ErrorResponse>(body, retrofitResponse)


    // [NetworkResponse] Network Errors
    val errorSocketTimeout =
        NetworkResponse.NetworkError<Unit, ErrorResponse>(SocketTimeoutException())
    val errorIOException =
        NetworkResponse.NetworkError<Unit, ErrorResponse>(IOException())

    // [NetworkResponse] Unknown Error
    val errorUnknown =
        NetworkResponse.UnknownError<String, ErrorResponse>(Throwable(), retrofitResponse)

    // [NetworkResponse] Server Error
    fun errorToFailure(code: String) = NetworkResponse.ServerError<Unit, ErrorResponse>(
        ErrorResponse(code, "", "", ""),
        retrofitResponse
    ).mapResponse().leftOrNull()

    context("Get the path of an HTTP Request and Response") {
        given("An HTTP request") {
            then("Get its path") {
                request.path() shouldBe path
            }
        }

        given("An HTTP response") {
            then("Get its path") {
                response.path() shouldBe path
            }
        }
    }

    context("Create Constraints with NetworkType = CONNECTED") {
        given("An extension helper function we have created") {
            then("Use it to create the constraints") {
                Constraints.Companion
                    .requireNetwork()
                    .requiredNetworkType shouldBe NetworkType.CONNECTED
            }
        }
    }

    context("Parse NetworkResponse<T, ErrorResponse> to Either<Failure, T>") {
        given("A NetworkResponse") {
            When("It's a success") {
                then("Parse it to Either.Right and return the body") {
                    successResponse.mapResponse() shouldBe Either.Right(body)
                }
            }
            When("It's a network error") {
                and("It's a SocketTimeoutException") {
                    then("Parse it to Either.Right and return ConnectionTimeoutError") {
                        errorSocketTimeout.mapResponse().leftOrNull()
                            .shouldBeTypeOf<NetworkError.ConnectionTimeoutError>()
                    }
                }
                and("It's a different IOException") {
                    then("Parse it to Either.Right and return NoConnectionError") {
                        errorIOException.mapResponse().leftOrNull()
                            .shouldBeTypeOf<NetworkError.NoConnectionError>()
                    }
                }
            }
            When("It's an Unknown Error") {
                then("Parse it to Either.Right and return UnknownError") {
                    errorUnknown.mapResponse().leftOrNull()
                        .shouldBeTypeOf<UnknownError>()
                }
            }
            When("It's an ErrorResponse (Server returned an error)") {
                and("It's an INVALID_USERNAME error") {
                    then("Parse it to Either.Left and return InvalidUsername") {
                        errorToFailure(ErrorResponse.INVALID_USERNAME)
                            .shouldBeTypeOf<ApiError.AuthError.InvalidUsername>()
                    }
                }
                and("It's an INVALID_PASSWORD error") {
                    then("Parse it to Either.Left and return InvalidPassword") {
                        errorToFailure(ErrorResponse.INVALID_PASSWORD)
                            .shouldBeTypeOf<ApiError.AuthError.LoginError.InvalidPassword>()
                    }
                }
                and("It's an INVALID_CREDENTIALS error") {
                    then("Parse it to Either.Left and return InvalidCredentials") {
                        errorToFailure(ErrorResponse.INVALID_CREDENTIALS)
                            .shouldBeTypeOf<ApiError.AuthError.LoginError.InvalidCredentials>()
                    }
                }
                and("It's an USER_ALREADY_EXISTS error") {
                    then("Parse it to Either.Left and return UserAlreadyExists") {
                        errorToFailure(ErrorResponse.USER_ALREADY_EXISTS)
                            .shouldBeTypeOf<ApiError.AuthError.SignupError.UserAlreadyExists>()
                    }
                }
                and("It's an INVALID_ACCESS_TOKEN error") {
                    then("Parse it to Either.Left and return InvalidAccessToken") {
                        errorToFailure(ErrorResponse.INVALID_ACCESS_TOKEN)
                            .shouldBeTypeOf<ApiError.AuthError.InvalidAccessToken>()
                    }
                }
                and("It's an INVALID_ACTIVATION_TOKEN error") {
                    then("Parse it to Either.Left and return InvalidActivationToken") {
                        errorToFailure(ErrorResponse.INVALID_ACTIVATION_TOKEN)
                            .shouldBeTypeOf<ApiError.AuthError.InvalidActivationToken>()
                    }
                }
                and("It's an DEVICE_NOT_FOUND error") {
                    then("Parse it to Either.Left and return DeviceNotFound") {
                        errorToFailure(ErrorResponse.DEVICE_NOT_FOUND)
                            .shouldBeTypeOf<ApiError.DeviceNotFound>()
                    }
                }
                and("It's an MAX_FOLLOWED error") {
                    then("Parse it to Either.Left and return MaxFollowed") {
                        errorToFailure(ErrorResponse.MAX_FOLLOWED)
                            .shouldBeTypeOf<ApiError.MaxFollowed>()
                    }
                }
                and("It's an INVALID_WALLET_ADDRESS error") {
                    then("Parse it to Either.Left and return InvalidWalletAddress") {
                        errorToFailure(ErrorResponse.INVALID_WALLET_ADDRESS)
                            .shouldBeTypeOf<ApiError.UserError.WalletError.InvalidWalletAddress>()
                    }
                }
                and("It's an INVALID_FRIENDLY_NAME error") {
                    then("Parse it to Either.Left and return InvalidFriendlyName") {
                        errorToFailure(ErrorResponse.INVALID_FRIENDLY_NAME)
                            .shouldBeTypeOf<ApiError.InvalidFriendlyName>()
                    }
                }
                and("It's an INVALID_FROM_DATE error") {
                    then("Parse it to Either.Left and return InvalidFromDate") {
                        errorToFailure(ErrorResponse.INVALID_FROM_DATE)
                            .shouldBeTypeOf<ApiError.UserError.InvalidFromDate>()
                    }
                }
                and("It's an INVALID_TO_DATE error") {
                    then("Parse it to Either.Left and return InvalidToDate") {
                        errorToFailure(ErrorResponse.INVALID_TO_DATE)
                            .shouldBeTypeOf<ApiError.UserError.InvalidToDate>()
                    }
                }
                and("It's an INVALID_TIMEZONE error") {
                    then("Parse it to Either.Left and return InvalidTimezone") {
                        errorToFailure(ErrorResponse.INVALID_TIMEZONE)
                            .shouldBeTypeOf<ApiError.UserError.InvalidTimezone>()
                    }
                }
                and("It's an INVALID_CLAIM_ID error") {
                    then("Parse it to Either.Left and return InvalidClaimId") {
                        errorToFailure(ErrorResponse.INVALID_CLAIM_ID)
                            .shouldBeTypeOf<ApiError.UserError.ClaimError.InvalidClaimId>()
                    }
                }
                and("It's an INVALID_CLAIM_LOCATION error") {
                    then("Parse it to Either.Left and return InvalidClaimLocation") {
                        errorToFailure(ErrorResponse.INVALID_CLAIM_LOCATION)
                            .shouldBeTypeOf<ApiError.UserError.ClaimError.InvalidClaimLocation>()
                    }
                }
                and("It's an DEVICE_ALREADY_CLAIMED error") {
                    then("Parse it to Either.Left and return DeviceAlreadyClaimed") {
                        errorToFailure(ErrorResponse.DEVICE_ALREADY_CLAIMED)
                            .shouldBeTypeOf<ApiError.UserError.ClaimError.DeviceAlreadyClaimed>()
                    }
                }
                and("It's an DEVICE_CLAIMING error") {
                    then("Parse it to Either.Left and return DeviceClaiming") {
                        errorToFailure(ErrorResponse.DEVICE_CLAIMING)
                            .shouldBeTypeOf<ApiError.UserError.ClaimError.DeviceClaiming>()
                    }
                }
                and("It's an UNAUTHORIZED error") {
                    then("Parse it to Either.Left and return Unauthorized") {
                        errorToFailure(ErrorResponse.UNAUTHORIZED)
                            .shouldBeTypeOf<UnauthorizedError>()
                    }
                }
                and("It's an USER_NOT_FOUND error") {
                    then("Parse it to Either.Left and return UserNotFoundError") {
                        errorToFailure(ErrorResponse.USER_NOT_FOUND)
                            .shouldBeTypeOf<UserNotFoundError>()
                    }
                }
                and("It's an FORBIDDEN error") {
                    then("Parse it to Either.Left and return ForbiddenError") {
                        errorToFailure(ErrorResponse.FORBIDDEN)
                            .shouldBeTypeOf<ForbiddenError>()
                    }
                }
                and("It's an VALIDATION error") {
                    then("Parse it to Either.Left and return ValidationError") {
                        errorToFailure(ErrorResponse.VALIDATION)
                            .shouldBeTypeOf<ValidationError>()
                    }
                }
                and("It's an NOT_FOUND error") {
                    then("Parse it to Either.Left and return NotFoundError") {
                        errorToFailure(ErrorResponse.NOT_FOUND)
                            .shouldBeTypeOf<NotFoundError>()
                    }
                }
                and("It's an WALLET_ADDRESS_NOT_FOUND error") {
                    then("Parse it to Either.Left and return WalletAddressNotFound") {
                        errorToFailure(ErrorResponse.WALLET_ADDRESS_NOT_FOUND)
                            .shouldBeTypeOf<WalletAddressNotFound>()
                    }
                }
                and("It's an UNSUPPORTED_APPLICATION_VERSION error") {
                    then("Parse it to Either.Left and return UnsupportedAppVersion") {
                        errorToFailure(ErrorResponse.UNSUPPORTED_APPLICATION_VERSION)
                            .shouldBeTypeOf<UnsupportedAppVersion>()
                    }
                }
                and("It's an UNKNOWN error") {
                    then("Parse it to Either.Left and return UnknownError") {
                        errorToFailure("").shouldBeTypeOf<UnknownError>()
                    }
                }
            }
        }
    }

    context("Manage Location to Text and Text to Location") {
        given("a location") {
            val location = Location.empty()
            then("Use it to create the respective formatted text") {
                location.locationToText() shouldBe "${location.lat}_${location.lon}"
            }
            then("parse a text to that location") {
                "${location.lat}_${location.lon}".textToLocation() shouldBe location
            }
        }
    }
})
