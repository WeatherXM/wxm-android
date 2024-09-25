package com.weatherxm.data.datasource

import com.haroldadmin.cnradapter.NetworkResponse
import com.weatherxm.TestConfig.failure
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.coMockNetworkError
import com.weatherxm.TestUtils.coMockNetworkSuccess
import com.weatherxm.TestUtils.isError
import com.weatherxm.TestUtils.isNetworkError
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.TestUtils.retrofitResponse
import com.weatherxm.data.models.Wallet
import com.weatherxm.data.network.AddressBody
import com.weatherxm.data.network.ApiService
import com.weatherxm.data.network.ErrorResponse
import com.weatherxm.data.services.CacheService
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.coJustRun
import io.mockk.mockk
import io.mockk.verify

class WalletDataSourceTest : BehaviorSpec({
    val apiService = mockk<ApiService>()
    val cacheService = mockk<CacheService>()
    val networkSource = NetworkWalletDataSource(apiService)
    val cacheSource = CacheWalletDataSource(cacheService)

    val address = "address"
    val wallet = Wallet(address, 0L)
    val successGetWalletResponse =
        NetworkResponse.Success<Wallet, ErrorResponse>(wallet, retrofitResponse(wallet))
    val successSetWalletResponse =
        NetworkResponse.Success<Unit, ErrorResponse>(Unit, retrofitResponse(Unit))
    val addressBody = AddressBody(address)

    beforeSpec {
        coJustRun { cacheService.setWalletAddress(address) }
    }

    context("GET / SET Wallet Addresses") {
        given("A Network and a Cache Source providing the wallet address") {
            When("Using the Network Source") {
                and("the response is a success") {
                    coMockNetworkSuccess({ apiService.getWallet() }, successGetWalletResponse)
                    then("return the address") {
                        networkSource.getWalletAddress().isSuccess(wallet.address)
                    }
                }
                and("the response is a failure") {
                    coMockNetworkError { apiService.getWallet() }
                    then("return the failure") {
                        networkSource.getWalletAddress().isNetworkError()
                    }
                }
            }
            When("Using the Cache Source") {
                and("the response is a success") {
                    coMockEitherRight({ cacheService.getWalletAddress() }, wallet.address)
                    then("return the address") {
                        cacheSource.getWalletAddress().isSuccess(wallet.address)
                    }
                }
                and("the response is a failure") {
                    coMockEitherLeft({ cacheService.getWalletAddress() }, failure)
                    then("return the failure") {
                        cacheSource.getWalletAddress().isError()
                    }
                }
            }
        }
        given("A Network and a Cache Source providing the SET mechanism") {
            When("Using the Network Source") {
                and("the response is a success") {
                    coMockNetworkSuccess(
                        { apiService.setWallet(addressBody) },
                        successSetWalletResponse
                    )
                    then("return Unit") {
                        networkSource.setWalletAddress(address).isSuccess(Unit)
                    }
                }
                and("the response is a failure") {
                    coMockNetworkError { apiService.setWallet(addressBody) }
                    then("return the failure") {
                        networkSource.getWalletAddress().isNetworkError()
                    }
                }
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
