package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.TestConfig.failure
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isError
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.data.datasource.CacheWalletDataSource
import com.weatherxm.data.datasource.NetworkWalletDataSource
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.core.test.isRootTest
import io.kotest.matchers.shouldBe
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk

class WalletRepositoryTest : BehaviorSpec({
    lateinit var networkSource: NetworkWalletDataSource
    lateinit var cacheSource: CacheWalletDataSource
    lateinit var repo: WalletRepository

    val walletAddress = "address"

    beforeInvocation { testCase, _ ->
        if (testCase.isRootTest()) {
            networkSource = mockk<NetworkWalletDataSource>()
            cacheSource = mockk<CacheWalletDataSource>()
            repo = WalletRepositoryImpl(networkSource, cacheSource)
            coJustRun { cacheSource.setWalletAddress(walletAddress) }
        }
    }

    context("Get the Wallet Address") {
        When("it's in the cache") {
            coMockEitherRight({ cacheSource.getWalletAddress() }, walletAddress)
            then("return the location") {
                repo.getWalletAddress().isSuccess(walletAddress)
            }
        }
        When("it's not in the cache") {
            coMockEitherLeft({ cacheSource.getWalletAddress() }, failure)
            and("we can get it from the network") {
                and("it's null") {
                    coMockEitherRight({ networkSource.getWalletAddress() }, null)
                    then("return null") {
                        repo.getWalletAddress().isSuccess(null)
                    }
                    then("do NOT save it in the cache") {
                        coVerify(exactly = 0) { cacheSource.setWalletAddress(walletAddress) }
                    }
                }
                and("it's not null") {
                    coMockEitherRight({ networkSource.getWalletAddress() }, walletAddress)
                    then("return the wallet address") {
                        repo.getWalletAddress().isSuccess(walletAddress)
                    }
                    then("save it in the cache") {
                        coVerify(exactly = 1) { cacheSource.setWalletAddress(walletAddress) }
                    }
                }
            }
            and("we can't get it from the network") {
                coMockEitherLeft({ networkSource.getWalletAddress() }, failure)
                then("return a failure") {
                    repo.getWalletAddress().isError()
                }
            }
        }
    }

    context("Set the Wallet Address") {
        given("a wallet address") {
            and("Set it using the network") {
                and("it's successful") {
                    coMockEitherRight({ networkSource.setWalletAddress(walletAddress) }, Unit)
                    then("return a success") {
                        repo.setWalletAddress(walletAddress).isSuccess(Unit)
                    }
                    then("save it in the cache") {
                        coVerify(exactly = 1) { cacheSource.setWalletAddress(walletAddress) }
                    }
                }
                and("it's not successful") {
                    coMockEitherLeft({ networkSource.setWalletAddress(walletAddress) }, failure)
                    then("return a failure") {
                        repo.setWalletAddress(walletAddress).isError()
                    }
                }
            }
        }
    }
})
