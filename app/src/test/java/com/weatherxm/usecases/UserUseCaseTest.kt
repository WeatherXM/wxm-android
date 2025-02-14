package com.weatherxm.usecases

import com.weatherxm.TestConfig.failure
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isError
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.data.models.User
import com.weatherxm.data.models.WalletRewards
import com.weatherxm.data.repository.RewardsRepository
import com.weatherxm.data.repository.UserPreferencesRepository
import com.weatherxm.data.repository.UserRepository
import com.weatherxm.data.repository.WalletRepository
import com.weatherxm.ui.common.UIWalletRewards
import com.weatherxm.ui.common.empty
import com.weatherxm.usecases.UserUseCaseImpl.Companion.WALLET_WARNING_DISMISS_EXPIRATION
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import java.util.concurrent.TimeUnit

class UserUseCaseTest : BehaviorSpec({
    val userRepository = mockk<UserRepository>()
    val userPreferencesRepository = mockk<UserPreferencesRepository>()
    val walletRepository = mockk<WalletRepository>()
    val rewardsRepository = mockk<RewardsRepository>()
    val usecase = UserUseCaseImpl(
        userRepository,
        userPreferencesRepository,
        walletRepository,
        rewardsRepository
    )

    val user = mockk<User>()
    val walletAddress = "address"
    val userId = "userId"
    val now = System.currentTimeMillis()
    val twoDaysAgo = now - TimeUnit.HOURS.toMillis(48L)
    val emptyWalletRewards = WalletRewards(null, null, null, null, null)
    val walletRewards = WalletRewards(null, 10.0, 0, 10.0, 10.0)

    beforeSpec {
        justRun { userPreferencesRepository.setWalletWarningDismissTimestamp() }
        justRun { userPreferencesRepository.setAcceptTerms() }
        justRun { userPreferencesRepository.setClaimingBadgeShouldShow(any()) }
        every { userPreferencesRepository.shouldShowAnalyticsOptIn() } returns true
        every { userPreferencesRepository.shouldShowTermsPrompt() } returns true
        every { userPreferencesRepository.getClaimingBadgeShouldShow() } returns true
    }

    context("Get the Wallet Address") {
        given("A repository providing the address") {
            When("it's a success") {
                and("the address returned is a null") {
                    coMockEitherRight({ walletRepository.getWalletAddress() }, null)
                    then("return an empty string") {
                        usecase.getWalletAddress().isSuccess(String.empty())
                    }
                }
                and("the address returned is not null") {
                    coMockEitherRight({ walletRepository.getWalletAddress() }, walletAddress)
                    then("return that address") {
                        usecase.getWalletAddress().isSuccess(walletAddress)
                    }
                }
            }
            When("it's a failure") {
                coMockEitherLeft({ walletRepository.getWalletAddress() }, failure)
                then("return that failure") {
                    usecase.getWalletAddress().isError()
                }
            }
        }
    }

    context("Set the Wallet Address") {
        given("A repository providing the SET functionality") {
            When("it's a success") {
                coMockEitherRight({ walletRepository.setWalletAddress(walletAddress) }, Unit)
                then("return the success") {
                    usecase.setWalletAddress(walletAddress).isSuccess(Unit)
                }
            }
            When("it's a failure") {
                coMockEitherLeft({ walletRepository.setWalletAddress(walletAddress) }, failure)
                then("return that failure") {
                    usecase.setWalletAddress(walletAddress).isError()
                }
            }
        }
    }

    context("Get User and User ID") {
        given("The repository providing the user") {
            When("it's a success") {
                coMockEitherRight({ userRepository.getUser() }, user)
                then("return the success") {
                    usecase.getUser().isSuccess(user)
                }
            }
            When("it's a failure") {
                coMockEitherLeft({ userRepository.getUser() }, failure)
                then("return that failure") {
                    usecase.getUser().isError()
                }
            }
        }
        given("The repository providing the user ID") {
            then("return the user's id") {
                every { userRepository.getUserId() } returns userId
                usecase.getUserId() shouldBe userId
            }
        }
    }

    context("Get if we should show the analytics opt in or not") {
        given("The repository which returns the answer") {
            then("return the answer") {
                usecase.shouldShowAnalyticsOptIn() shouldBe true
            }
        }
    }

    context("Set Wallet Warning Dismiss Timestamp") {
        given("A repository providing the SET functionality") {
            then("make the respective call") {
                usecase.setWalletWarningDismissTimestamp()
                verify(exactly = 1) { userPreferencesRepository.setWalletWarningDismissTimestamp() }
            }
        }
    }

    context("Get if we should show the wallet missing warning or not") {
        given("A repository providing which the last dismiss timestamp is") {
            When("Wallet Address is empty") {
                and("the $WALLET_WARNING_DISMISS_EXPIRATION hasn't passed yet") {
                    every {
                        userPreferencesRepository.getWalletWarningDismissTimestamp()
                    } returns now
                    then("return false") {
                        usecase.shouldShowWalletMissingWarning(String.empty()) shouldBe false
                    }
                }
                and("the $WALLET_WARNING_DISMISS_EXPIRATION has passed") {
                    every {
                        userPreferencesRepository.getWalletWarningDismissTimestamp()
                    } returns twoDaysAgo
                    then("return true") {
                        usecase.shouldShowWalletMissingWarning(String.empty()) shouldBe true
                    }
                }
            }
            When("Wallet Address is NOT empty") {
                then("return false") {
                    usecase.shouldShowWalletMissingWarning(walletAddress) shouldBe false
                }
            }
        }
    }

    context("Get the wallet rewards") {
        given("A repository providing the wallet rewards") {
            When("Wallet Address is empty") {
                then("return empty rewards") {
                    usecase.getWalletRewards(String.empty()).isSuccess(UIWalletRewards.empty())
                }
            }
            When("Wallet Address is null") {
                then("return empty rewards") {
                    usecase.getWalletRewards(null).isSuccess(UIWalletRewards.empty())
                }
            }
            When("Wallet Address is NOT empty") {
                and("it's a success") {
                    and("the rewards returned are empty") {
                        coMockEitherRight(
                            { rewardsRepository.getWalletRewards(walletAddress) },
                            emptyWalletRewards
                        )
                        then("return the default rewards") {
                            usecase.getWalletRewards(walletAddress).isSuccess(
                                UIWalletRewards(0.0, 0.0, 0.0, walletAddress)
                            )
                        }
                    }
                    and("the rewards returned are not empty") {
                        coMockEitherRight(
                            { rewardsRepository.getWalletRewards(walletAddress) },
                            walletRewards
                        )
                        then("return the rewards") {
                            usecase.getWalletRewards(walletAddress).isSuccess(
                                UIWalletRewards(10.0, 10.0, 10.0, walletAddress)
                            )
                        }
                    }
                }
                and("it's a failure") {
                    coMockEitherLeft({ rewardsRepository.getWalletRewards(walletAddress) }, failure)
                    then("return that failure") {
                        usecase.getWalletRewards(walletAddress).isError()
                    }
                }
            }
        }
    }

    context("Get if we should show the terms prompt or not") {
        given("The repository which returns the answer") {
            then("return the answer") {
                usecase.shouldShowTermsPrompt() shouldBe true
            }
        }
    }

    context("Set the Accept Terms") {
        given("A repository providing the SET functionality") {
            usecase.setAcceptTerms()
            then("make the respective call") {
                verify(exactly = 1) { userPreferencesRepository.setAcceptTerms() }
            }
        }
    }

    context("Get if we should show the badge for the claiming or not") {
        given("The repository which returns the answer") {
            then("return the answer") {
                usecase.getClaimingBadgeShouldShow() shouldBe true
            }
        }
    }

    context("Set if we should show the badge for the claiming or not") {
        given("A repository providing the SET functionality") {
            usecase.setClaimingBadgeShouldShow(true)
            then("make the respective call") {
                verify(exactly = 1) { userPreferencesRepository.setClaimingBadgeShouldShow(true) }
            }
        }
    }
})
