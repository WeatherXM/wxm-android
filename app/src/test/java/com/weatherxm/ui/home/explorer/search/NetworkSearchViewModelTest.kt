package com.weatherxm.ui.home.explorer.search

import com.weatherxm.TestConfig.REACH_OUT_MSG
import com.weatherxm.TestConfig.dispatcher
import com.weatherxm.TestConfig.failure
import com.weatherxm.TestConfig.resources
import com.weatherxm.TestUtils.coMockEitherLeft
import com.weatherxm.TestUtils.coMockEitherRight
import com.weatherxm.TestUtils.isSuccess
import com.weatherxm.TestUtils.testHandleFailureViewModel
import com.weatherxm.analytics.AnalyticsWrapper
import com.weatherxm.ui.InstantExecutorListener
import com.weatherxm.ui.common.empty
import com.weatherxm.ui.home.explorer.SearchResult
import com.weatherxm.usecases.ExplorerUseCase
import com.weatherxm.util.Resources
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.justRun
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class NetworkSearchViewModelTest : BehaviorSpec({
    val usecase = mockk<ExplorerUseCase>()
    val analytics = mockk<AnalyticsWrapper>()
    lateinit var viewModel: NetworkSearchViewModel

    val query = "query"
    val searchResult = mockk<SearchResult>()
    val searchResults = listOf(searchResult)

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
        coJustRun { usecase.setRecentSearch(searchResult) }
        coEvery { usecase.getRecentSearches() } returns searchResults

        viewModel = NetworkSearchViewModel(
            usecase,
            analytics,
            dispatcher
        )
    }

    context("GET / SET the query") {
        When("GET the query") {
            then("return the default empty query") {
                viewModel.getQuery() shouldBe String.empty()
            }
        }
        When("SET a new query") {
            viewModel.setQuery(query)
            then("GET it to ensure it has been set") {
                viewModel.getQuery() shouldBe query
            }
        }
    }

    context("Perform a network search") {
        given("a usecase returning the results of the network search") {
            viewModel.setQuery(query)
            When("it's a new query") {
                and("it's a success") {
                    coMockEitherRight({ usecase.networkSearch(query) }, searchResults)
                    runTest { viewModel.networkSearch() }
                    then("LiveData onSearchResults should post the respective results we fetched") {
                        viewModel.onSearchResults().isSuccess(searchResults)
                    }
                }
            }
            When("the query remains the same") {
                coMockEitherRight({ usecase.networkSearch(query) }, listOf<SearchResult>())
                then("do nothing, don't perform another network search, LiveData keeps its value") {
                    runTest { viewModel.networkSearch() }
                    viewModel.onSearchResults().isSuccess(searchResults)
                }
            }
            When("we want to cancel the current network search query") {
                then("LiveData posts its latest value saved from the network search") {
                    runTest { viewModel.cancelNetworkSearchJob(true) }
                    viewModel.onSearchResults().isSuccess(searchResults)
                }
            }
            When("we use another new query") {
                viewModel.setQuery(String.empty())
                and("it's a failure") {
                    coMockEitherLeft({ usecase.networkSearch(String.empty()) }, failure)
                    /**
                     * Here we set runImmediately = true so we don't need to wait for the delay
                     */
                    testHandleFailureViewModel(
                        { viewModel.networkSearch(true) },
                        analytics,
                        viewModel.onSearchResults(),
                        1,
                        REACH_OUT_MSG
                    )
                }
            }
        }
    }

    context("Get the recent searches") {
        given("a usecase returning the recent searches") {
            runTest { viewModel.getRecentSearches() }
            then("return them through LiveData") {
                viewModel.onRecentSearches().value shouldBe searchResults
            }
        }
    }

    context("Flow to be called when a search has been clicked/selected") {
        given("the selected SearchResult") {
            runTest { viewModel.onSearchClicked(searchResult) }
            then("ensure that the usecase has been called setting this as a recent search") {
                coVerify(exactly = 1) { usecase.setRecentSearch(searchResult) }
            }
            then("set query to empty") {
                viewModel.getQuery() shouldBe String.empty()
            }
        }
    }

    afterSpec {
        stopKoin()
    }
})
