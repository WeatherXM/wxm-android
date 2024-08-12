package com.weatherxm.data.repository

import arrow.core.Either
import com.weatherxm.TestConfig.failure
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isError
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.data.User
import com.weatherxm.data.datasource.CacheUserDataSource
import com.weatherxm.data.datasource.NetworkUserDataSource
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk

class UserRepositoryTest : BehaviorSpec({
    lateinit var networkSource: NetworkUserDataSource
    lateinit var cacheSource: CacheUserDataSource
    lateinit var repository: UserRepository

    val username = "username@email.com"
    val userId = "userId"
    val user = mockk<User> {
        every { email } returns username
    }

    beforeContainer {
        networkSource = mockk<NetworkUserDataSource>()
        cacheSource = mockk<CacheUserDataSource>()
        repository = UserRepositoryImpl(networkSource, cacheSource)
        every { cacheSource.getUserId() } returns userId
        coJustRun { cacheSource.setUser(user) }
        coJustRun { cacheSource.setUserUsername(username) }
        coMockEitherRight({ networkSource.getUser() }, user)
    }

    context("Perform user-related actions") {
        When("requesting the user's username") {
            and("it's a success") {
                then("return the username") {
                    coMockEitherRight({ cacheSource.getUserUsername() }, username)
                    repository.getUserUsername().isSuccess(username)
                }
            }
            and("it's a failure") {
                then("return that failure") {
                    coMockEitherLeft({ cacheSource.getUserUsername() }, failure)
                    repository.getUserUsername().isError()
                }
            }
        }
        When("requesting the user's id") {
            then("return the user's id") {
                repository.getUserId() shouldBe userId
            }
        }
        When("requesting to delete the account") {
            and("it's a success") {
                then("return the Unit indicating that it was a success") {
                    coMockEitherRight({ networkSource.deleteAccount() }, Unit)
                    repository.deleteAccount().isSuccess(Unit)
                }
            }
            and("it's a failure") {
                then("return that failure") {
                    coMockEitherLeft({ networkSource.deleteAccount() }, failure)
                    repository.deleteAccount().isError()
                }
            }
        }
        When("requesting the user") {
            and("force refresh is false (fetching from cache)") {
                and("it's a success") {
                    then("return the user") {
                        coMockEitherRight({ cacheSource.getUser() }, user)
                        repository.getUser(false).isSuccess(user)
                    }
                }
                and("it's a failure") {
                    then("fetch from network") {
                        coMockEitherLeft({ cacheSource.getUser() }, failure)
                        repository.getUser(false).isSuccess(user)
                        coVerify(exactly = 1) { networkSource.getUser() }
                    }
                }
            }
            and("force refresh is true (fetching from network)") {
                and("it's a success") {
                    then("return the user") {
                        repository.getUser(true).isSuccess(user)
                    }
                    then("save the user in the cache") {
                        coVerify(exactly = 1) { cacheSource.setUser(user) }
                        coVerify(exactly = 1) { cacheSource.setUserUsername(username) }
                    }
                }
                and("it's a failure") {
                    then("return that failure") {
                        coMockEitherLeft({ networkSource.getUser() }, failure)
                        repository.getUser(true).isError()
                    }
                }
            }
        }

    }

})
