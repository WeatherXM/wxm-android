package com.weatherxm.data.datasource

import com.haroldadmin.cnradapter.NetworkResponse
import com.weatherxm.TestConfig.successUnitResponse
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.TestUtils.retrofitResponse
import com.weatherxm.TestUtils.testGetFromCache
import com.weatherxm.TestUtils.testNetworkCall
import com.weatherxm.data.models.Wallet
import com.weatherxm.data.network.AddressBody
import com.weatherxm.data.network.ApiService
import com.weatherxm.data.network.ErrorResponse
import com.weatherxm.data.services.CacheService
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.coEvery
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
                testGetFromCache(
                    "address",
                    wallet.address,
                    mockFunction = { cacheService.getWalletAddress() },
                    runFunction = { cacheSource.getWalletAddress() }
                )
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
