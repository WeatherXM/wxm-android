package com.weatherxm.ui.connectwallet

import com.weatherxm.R
import com.weatherxm.TestConfig.REACH_OUT_MSG
import com.weatherxm.TestConfig.dispatcher
import com.weatherxm.TestConfig.failure
import com.weatherxm.TestConfig.resources
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.TestUtils.testHandleFailureViewModel
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.data.models.ApiError
import com.weatherxm.ui.InstantExecutorListener
import com.weatherxm.ui.connectwallet.ConnectWalletViewModel.Companion.ETH_ADDR_PREFIX
import com.weatherxm.ui.connectwallet.ConnectWalletViewModel.Companion.LAST_CHARS_TO_SHOW_AS_CONFIRM
import com.weatherxm.usecases.UserUseCase
import com.weatherxm.util.Resources
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class ConnectWalletViewModelTest : BehaviorSpec({
    val usecase = mockk<UserUseCase>()
    val analytics = mockk<AnalyticsWrapper>()
    lateinit var viewModel: ConnectWalletViewModel

    val addressSavedMsg = "Address Saved"
    val invalidAddressMsg = "Invalid Address"
    val address = "0x0123456789ABCDEF000000000000000000000000"
    val addressWithMetamaskPrefix = "metamask:0x0123456789ABCDEF000000000000000000000000"
    val invalidAddress = "000123456789ABCDEF000000000000000000000000"
    val invalidWalletAddressFailure = ApiError.UserError.WalletError.InvalidWalletAddress("")

    listener(InstantExecutorListener())

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
        justRun { analytics.trackEventFailure(any()) }
        coMockEitherRight({ usecase.getWalletAddress() }, address)
        every { resources.getString(R.string.address_saved) } returns addressSavedMsg
        every {
            resources.getString(R.string.error_connect_wallet_invalid_address)
        } returns invalidAddressMsg

        viewModel = ConnectWalletViewModel(usecase, resources, analytics, dispatcher)
    }

    context("Set a new wallet address") {
        given("a usecase which we give the address and it propagates it") {
            When("it's a failure") {
                and("It's an InvalidWalletAddress failure") {
                    coMockEitherLeft(
                        { usecase.setWalletAddress(address) },
                        invalidWalletAddressFailure
                    )
                    testHandleFailureViewModel(
                        { viewModel.setWalletAddress(address) },
                        analytics,
                        viewModel.isAddressSaved(),
                        1,
                        invalidAddressMsg
                    )
                }
                and("it's any other failure") {
                    coMockEitherLeft({ usecase.setWalletAddress(address) }, failure)
                    testHandleFailureViewModel(
                        { viewModel.setWalletAddress(address) },
                        analytics,
                        viewModel.isAddressSaved(),
                        2,
                        REACH_OUT_MSG
                    )
                }
            }
            When("it's a success") {
                coMockEitherRight({ usecase.setWalletAddress(address) }, Unit)
                runTest { viewModel.setWalletAddress(address) }
                then("LiveData at isAddressSaved posts a success") {
                    viewModel.isAddressSaved().isSuccess(addressSavedMsg)
                }
                then("LiveData at currentAddress posts a success with the address") {
                    viewModel.currentAddress().value shouldBe address
                }
            }
        }
    }

    context("Get the last part of the address containing $LAST_CHARS_TO_SHOW_AS_CONFIRM chars") {
        given("an address") {
            then("return the respective substring") {
                viewModel.getLastPartOfAddress(address) shouldBe address.takeLast(
                    LAST_CHARS_TO_SHOW_AS_CONFIRM
                )
            }
        }
    }

    context("Get a scanned address from a QR and return it in a valid form") {
        given("a QR-scanned address") {
            When("it is empty") {
                then("return null") {
                    viewModel.onScanAddress("") shouldBe null
                }
            }
            When("it is null") {
                then("return null") {
                    viewModel.onScanAddress(null) shouldBe null
                }
            }
            When("it doesn't contain the $ETH_ADDR_PREFIX (0x)") {
                then("return null") {
                    viewModel.onScanAddress(invalidAddress) shouldBe null
                }
            }
            When("it contains the $ETH_ADDR_PREFIX (0x)") {
                then("return the address starting from the $ETH_ADDR_PREFIX (0x) prefix") {
                    viewModel.onScanAddress(addressWithMetamaskPrefix) shouldBe address
                }
            }
        }
    }

    afterSpec {
        stopKoin()
    }
})
