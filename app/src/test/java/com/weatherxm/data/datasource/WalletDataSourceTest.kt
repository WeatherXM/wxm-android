package com.weatherxm.data.datasource

import arrow.core.Either
import com.haroldadmin.cnradapter.NetworkResponse
import com.weatherxm.TestConfig.cacheService
import com.weatherxm.TestConfig.failure
import com.weatherxm.TestConfig.successUnitResponse
import com.weatherxm.TestUtils.isError
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.TestUtils.retrofitResponse
import com.weatherxm.TestUtils.testNetworkCall
import com.weatherxm.data.models.Wallet
import com.weatherxm.data.network.AddressBody
import com.weatherxm.data.network.ApiService
import com.weatherxm.data.network.ErrorResponse
import com.weatherxm.data.services.CacheService
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.coJustRun
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class WalletDataSourceTest : BehaviorSpec({
    val apiService = mockk<ApiService>()
    val networkSource = NetworkWalletDataSource(apiService)
    val cacheSource = CacheWalletDataSource(cacheService)

    val address = "address"
    val wallet = Wallet(address, 0L)
    val successGetWalletResponse =
        NetworkResponse.Success<Wallet, ErrorResponse>(wallet, retrofitResponse(wallet))
    val addressBody = AddressBody(address)

    beforeSpec {
        coJustRun { cacheService.setWalletAddress(address) }
    }

    context("Get / Set Wallet Addresses") {
        given("A Network and a Cache Source providing the wallet address") {
            When("Using the Network Source") {
                testNetworkCall(
                    "Wallet Address",
                    wallet.address,
                    successGetWalletResponse,
                    mockFunction = { apiService.getWallet() },
                    runFunction = { networkSource.getWalletAddress() }
                )
            }
            When("Using the Cache Source") {
                /**
                 * Avoid using testGetFromCache because we get an invalid warning, more info:
                 * https://github.com/mockk/mockk/issues/1291
                 *
                 * So we use property-backing fields: https://mockk.io/#property-backing-fields
                 */
                and("the response is a success") {
                    every {
                        cacheService.getWalletAddress()
                    } propertyType Either::class answers { Either.Right(wallet.address) }
                    then("return the address") {
                        cacheSource.getWalletAddress().isSuccess(wallet.address)
                    }
                }
                and("the response is a failure") {
                    every {
                        cacheService.getWalletAddress()
                    } propertyType Either::class answers { Either.Left(failure) }
                    then("return the failure") {
                        cacheSource.getWalletAddress().isError()
                    }
                }
            }
        }
        given("A Network and a Cache Source providing the SET mechanism") {
            When("Using the Network Source") {
                testNetworkCall(
                    "Unit",
                    Unit,
                    successUnitResponse,
                    mockFunction = { apiService.setWallet(addressBody) },
                    runFunction = { networkSource.setWalletAddress(address) }
                )
            }
            When("Using the Cache Source") {
                then("Set it in cache and return Unit") {
                    cacheSource.setWalletAddress(address).isSuccess(Unit)
                    verify(exactly = 1) { cacheService.setWalletAddress(address) }
                }
            }
        }
    }
})
