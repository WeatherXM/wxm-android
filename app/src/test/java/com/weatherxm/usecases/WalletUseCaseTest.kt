package com.weatherxm.usecases

import com.weatherxm.TestConfig.failure
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isError
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.data.repository.WalletRepository
import com.weatherxm.ui.common.empty
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.mockk

class WalletUseCaseTest : BehaviorSpec({
    val repo = mockk<WalletRepository>()
    val usecase = WalletUseCaseImpl(repo)

    val walletAddress = "address"

    context("Get the Wallet Address") {
        given("A repository providing the address") {
            When("it's a success") {
                and("the address returned is a null") {
                    coMockEitherRight({ repo.getWalletAddress() }, null)
                    then("return an empty string") {
                        usecase.getWalletAddress().isSuccess(String.empty())
                    }
                }
                and("the address returned is not null") {
                    coMockEitherRight({ repo.getWalletAddress() }, walletAddress)
                    then("return that address") {
                        usecase.getWalletAddress().isSuccess(walletAddress)
                    }
                }
            }
            When("it's a failure") {
                coMockEitherLeft({ repo.getWalletAddress() }, failure)
                then("return that failure") {
                    usecase.getWalletAddress().isError()
                }
            }
        }
    }

    context("Set the Wallet Address") {
        given("A repository providing the set functionality") {
            When("it's a success") {
                coMockEitherRight({ repo.setWalletAddress(walletAddress) }, Unit)
                then("return the success") {
                    usecase.setWalletAddress(walletAddress).isSuccess(Unit)
                }
            }
            When("it's a failure") {
                coMockEitherLeft({ repo.setWalletAddress(walletAddress) }, failure)
                then("return that failure") {
                    usecase.setWalletAddress(walletAddress).isError()
                }
            }
        }
    }
})
